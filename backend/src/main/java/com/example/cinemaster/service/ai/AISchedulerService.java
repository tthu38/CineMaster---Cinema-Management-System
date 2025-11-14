        package com.example.cinemaster.service.ai;

import com.example.cinemaster.entity.WorkSchedule;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.ShiftRequest;
import com.example.cinemaster.repository.*;
        import com.example.cinemaster.service.ai.core.*;
        import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AISchedulerService {

    private final AccountRepository accountRepo;
    private final ShiftRequestRepository shiftReqRepo;
    private final WorkScheduleRepository scheduleRepo;

    // AI modules
    private final GeneticAlgorithm ga = new GeneticAlgorithm();
    private final SimulatedAnnealing sa = new SimulatedAnnealing();
    private final ReinforcementLearning rl; // <-- Spring sẽ inject tự động

    /** ===========================
     *  TẠO LỊCH 7 NGÀY (AI)
     ============================ */
    @Transactional
    public List<WorkSchedule> generateWeeklySchedule(Integer branchId, LocalDate weekStart) {

        SchedulingContext ctx = buildContext(branchId, weekStart);

        Chromosome solution = ga.run(ctx);
        solution = sa.optimize(solution, ctx);
        solution = rl.adjust(solution, ctx);   // <-- dùng bean RL của Spring

        List<WorkSchedule> finalSchedules = convertToEntities(solution, ctx);

        scheduleRepo.deleteByBranch_IdAndShiftDateBetween(branchId, weekStart, weekStart.plusDays(6));

        return scheduleRepo.saveAll(finalSchedules);
    }

    /** ===========================
     *  GET Repo cho Controller
     ============================ */
    public WorkScheduleRepository getScheduleRepository() {
        return scheduleRepo;
    }

    /** ===========================
     *  B1: Build Context
     ============================ */
    private SchedulingContext buildContext(Integer branchId, LocalDate weekStart) {

        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) weekDates.add(weekStart.plusDays(i));

        List<Account> staff = accountRepo.findAllByBranch_Id(branchId);

        List<ShiftRequest> shiftReq = shiftReqRepo
                .findByBranch_IdAndShiftDateBetween(branchId, weekStart, weekStart.plusDays(6));

        return SchedulingContext.builder()
                .branchId(branchId)
                .weekStart(weekStart)
                .weekDates(weekDates)
                .shiftTypes(List.of("MORNING", "AFTERNOON", "NIGHT"))
                .requiredPerShift(buildDefaultRequirements())  // số người mỗi ca
                .staff(staff)
                .shiftRequests(shiftReq)
                .build();
    }

    /** ===========================
     *  B2: Convert Chromosome → Entities
     ============================ */
    private List<WorkSchedule> convertToEntities(Chromosome c, SchedulingContext ctx) {

        List<WorkSchedule> list = new ArrayList<>();

        for (Gene g : c.getGenes()) {

            LocalDate date = LocalDate.parse(g.getDate());
            String shift = g.getShiftType();

            String[] hours = switch (shift) {
                case "MORNING" -> new String[]{"08:00", "13:00"};
                case "AFTERNOON" -> new String[]{"13:00", "18:00"};
                default -> new String[]{"18:00", "23:00"};
            };

            WorkSchedule ws = WorkSchedule.builder()
                    .branch(ctx.getStaff().get(0).getBranch())
                    .account(ctx.getStaff().stream()
                            .filter(s -> s.getAccountID().equals(g.getStaffId()))
                            .findFirst()
                            .orElseThrow())
                    .shiftDate(date)
                    .shiftType(shift)
                    .startTime(java.time.LocalTime.parse(hours[0]))
                    .endTime(java.time.LocalTime.parse(hours[1]))
                    .note("AI Generated")
                    .build();

            list.add(ws);
        }

        return list;
    }

    /** ===========================
     *  Nhân lực mặc định mỗi ca
     ============================ */
    private Map<String, Integer> buildDefaultRequirements() {

        Map<String, Integer> map = new HashMap<>();

        for (DayOfWeek d : DayOfWeek.values()) {
            String day = d.toString();
            map.put(day + "_MORNING", 2);
            map.put(day + "_AFTERNOON", 2);
            map.put(day + "_NIGHT", 2);
        }

        // Thứ 7 + Chủ nhật → nhiều khách
        List.of("SATURDAY", "SUNDAY").forEach(d -> {
            map.put(d + "_MORNING", 3);
            map.put(d + "_AFTERNOON", 3);
            map.put(d + "_NIGHT", 3);
        });

        // Thứ 2 → ít khách
        map.put("MONDAY_MORNING", 1);
        map.put("MONDAY_AFTERNOON", 1);

        return map;
    }

    public List<WorkSchedule> getGeneratedSchedule(Integer branchId, LocalDate weekStart) {
        return scheduleRepo.findByBranch_IdAndShiftDateBetween(
                branchId,
                weekStart,
                weekStart.plusDays(6)
        );
    }

}



