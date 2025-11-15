package com.example.cinemaster.service.ai;


import com.example.cinemaster.dto.request.AiPreviewSaveRequest;
import com.example.cinemaster.dto.response.AiPreviewResponse;
import com.example.cinemaster.dto.response.AiStaffResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;


import com.example.cinemaster.service.ai.core.*;
import com.example.cinemaster.service.ai.dto.SchedulingContext;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class AISchedulerService {


    private final AccountRepository accountRepo;
    private final BranchRepository branchRepo;
    private final ShiftRequestRepository shiftReqRepo;
    private final WorkScheduleRepository scheduleRepo;


    private final GeneticAlgorithm ga = new GeneticAlgorithm();
    private final SimulatedAnnealing sa = new SimulatedAnnealing();
    private final ReinforcementLearning rl;


    /* ============================================================
     * 1) PREVIEW
     * ============================================================ */
    public AiPreviewResponse generatePreviewSchedule(Integer branchId, LocalDate weekStart) {


        SchedulingContext ctx = buildContext(branchId, weekStart);


        Chromosome sol = ga.run(ctx);
        sol = sa.optimize(sol, ctx);
        sol = rl.adjust(sol, ctx);


        AiPreviewResponse res = new AiPreviewResponse();
        res.setMatrix(buildMatrix(sol, ctx));
        return res;
    }


    private Map<String, Map<String, List<AiStaffResponse>>> buildMatrix(
            Chromosome c, SchedulingContext ctx) {


        Map<String, Map<String, List<AiStaffResponse>>> map = new TreeMap<>();


        for (Gene g : c.getGenes()) {


            map.putIfAbsent(g.getDate(), new HashMap<>());
            map.get(g.getDate()).putIfAbsent("MORNING", new ArrayList<>());
            map.get(g.getDate()).putIfAbsent("AFTERNOON", new ArrayList<>());
            map.get(g.getDate()).putIfAbsent("NIGHT", new ArrayList<>());


            Account st = ctx.getStaff().stream()
                    .filter(s -> s.getAccountID().equals(g.getStaffId()))
                    .findFirst().orElse(null);


            if (st != null) {
                map.get(g.getDate()).get(g.getShiftType())
                        .add(new AiStaffResponse(st.getAccountID(), st.getFullName()));
            }
        }
        return map;
    }




    /* ============================================================
     * 2) SAVE AI SCHEDULE (KHÔNG RETURN ENTITY)
     * ============================================================ */
    @Transactional
    public void saveGeneratedSchedule(Integer branchId, LocalDate weekStart, AiPreviewSaveRequest req) {


        // Xóa lịch cũ
        scheduleRepo.deleteByBranch_IdAndShiftDateBetweenAndNote(
                branchId,
                weekStart,
                weekStart.plusDays(6),
                "AI Generated"
        );




        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));


        List<WorkSchedule> list = new ArrayList<>();


        // === Tạo lịch AI ===
        req.getMatrix().forEach((dateStr, shifts) -> {


            LocalDate date = LocalDate.parse(dateStr);


            shifts.forEach((shift, staffIds) -> {
                if (staffIds == null) return;


                for (Integer id : staffIds) {


                    Account acc = accountRepo.findById(id)
                            .orElseThrow(() -> new RuntimeException("Invalid staff id: " + id));


                    // ❗ Không ghi đè ca đã được duyệt
                    boolean existsApproved =
                            shiftReqRepo.existsByBranch_IdAndAccount_AccountIDAndShiftDateAndShiftTypeAndStatus(
                                    branchId,
                                    id,
                                    date,
                                    shift,
                                    "APPROVED"
                            );


                    if (existsApproved) {
                        continue; // đã có ca được duyệt → không tạo AI Generated
                    }




                    String[] h = switch (shift) {
                        case "MORNING" -> new String[]{"08:00", "13:00"};
                        case "AFTERNOON" -> new String[]{"13:00", "18:00"};
                        default -> new String[]{"18:00", "23:00"};
                    };


                    list.add(
                            WorkSchedule.builder()
                                    .branch(branch)
                                    .account(acc)
                                    .shiftDate(date)
                                    .shiftType(shift)
                                    .startTime(LocalTime.parse(h[0]))
                                    .endTime(LocalTime.parse(h[1]))
                                    .note("AI Generated")
                                    .build()
                    );
                }


            });
        });


        scheduleRepo.saveAll(list);


        // =====================================================
        // 🔥 APPROVE request khớp lịch AI
        //    Không đổi request khác → vẫn giữ PENDING
        // =====================================================
        List<ShiftRequest> requests = shiftReqRepo
                .findByBranch_IdAndShiftDateBetween(branchId, weekStart, weekStart.plusDays(6));


        for (ShiftRequest r : requests) {


            boolean matched = list.stream().anyMatch(ws ->
                    ws.getAccount().getAccountID().equals(r.getAccount().getAccountID()) &&
                            ws.getShiftDate().equals(r.getShiftDate()) &&
                            ws.getShiftType().equalsIgnoreCase(r.getShiftType())
            );


            if (matched) {
                r.setStatus("APPROVED");
            }
        }


        shiftReqRepo.saveAll(requests);
    }


    /* ============================================================
     * 3) LOAD AI SAVED SCHEDULE
     * ============================================================ */
    public AiPreviewResponse getGeneratedSchedule(Integer branchId, LocalDate weekStart) {


        List<WorkSchedule> wsList = scheduleRepo.findByBranch_IdAndShiftDateBetween(
                branchId,
                weekStart,
                weekStart.plusDays(6)
        );


        Map<String, Map<String, List<AiStaffResponse>>> matrix = new TreeMap<>();


        for (WorkSchedule ws : wsList) {


            String date = ws.getShiftDate().toString();
            String shift = ws.getShiftType();


            matrix.putIfAbsent(date, new HashMap<>());
            matrix.get(date).putIfAbsent("MORNING", new ArrayList<>());
            matrix.get(date).putIfAbsent("AFTERNOON", new ArrayList<>());
            matrix.get(date).putIfAbsent("NIGHT", new ArrayList<>());


            matrix.get(date).get(shift).add(
                    new AiStaffResponse(
                            ws.getAccount().getAccountID(),
                            ws.getAccount().getFullName()
                    )
            );
        }


        AiPreviewResponse res = new AiPreviewResponse();
        res.setMatrix(matrix);
        return res;
    }




    /* ============================================================
     * BUILD CONTEXT
     * ============================================================ */
    private SchedulingContext buildContext(Integer branchId, LocalDate weekStart) {


        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) weekDates.add(weekStart.plusDays(i));


        List<ShiftRequest> shiftReq = shiftReqRepo
                .findByBranch_IdAndShiftDateBetween(branchId, weekStart, weekStart.plusDays(6));


        Set<Integer> ids = new HashSet<>();
        for (ShiftRequest r : shiftReq) ids.add(r.getAccount().getAccountID());


        List<Account> staff = accountRepo.findAllByBranch_Id(branchId).stream()
                .filter(a -> ids.contains(a.getAccountID()))
                .toList();


        return SchedulingContext.builder()
                .branchId(branchId)
                .weekStart(weekStart)
                .weekDates(weekDates)
                .shiftRequests(shiftReq)
                .staff(staff)
                .shiftTypes(List.of("MORNING", "AFTERNOON", "NIGHT"))
                .requiredPerShift(defaultReq())
                .build();
    }


    private Map<String, Integer> defaultReq() {
        Map<String, Integer> m = new HashMap<>();
        for (var d : java.time.DayOfWeek.values()) {
            m.put(d + "_MORNING", 2);
            m.put(d + "_AFTERNOON", 2);
            m.put(d + "_NIGHT", 2);
        }
        m.put("SATURDAY_MORNING", 3);
        m.put("SATURDAY_AFTERNOON", 3);
        m.put("SATURDAY_NIGHT", 3);


        m.put("SUNDAY_MORNING", 3);
        m.put("SUNDAY_AFTERNOON", 3);
        m.put("SUNDAY_NIGHT", 3);


        return m;
    }
}

