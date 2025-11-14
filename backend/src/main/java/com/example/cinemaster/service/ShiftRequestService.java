package com.example.cinemaster.service;


import com.example.cinemaster.dto.request.ShiftRequestCreateRequest;
import com.example.cinemaster.dto.response.ShiftRequestResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.ShiftRequest;
import com.example.cinemaster.entity.WorkSchedule;
import com.example.cinemaster.mapper.ShiftRequestMapper;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.BranchRepository;
import com.example.cinemaster.repository.ShiftRequestRepository;
import com.example.cinemaster.repository.WorkScheduleRepository;
import com.example.cinemaster.security.AccountPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ShiftRequestService {


    private final ShiftRequestRepository repo;
    private final AccountRepository accountRepo;
    private final BranchRepository branchRepo;
    private final ShiftRequestMapper mapper;
    private final WorkScheduleRepository workScheduleRepository;




    /**
     * Tạo nhiều ShiftRequest từ 1 request (FE gửi list shifts[])
     */
    public List<ShiftRequestResponse> createRequest(ShiftRequestCreateRequest req) {


        // Lấy account nhân viên đăng nhập
        var principal = (AccountPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();


        Account employee = accountRepo.getReferenceById(principal.getId());


        // Lấy chi nhánh
        Branch branch = branchRepo.findById(req.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + req.getBranchId()));


        // Lấy danh sách manager
        List<Account> managers =
                accountRepo.findAllByBranch_IdAndRole_RoleName(req.getBranchId(), "Manager");


        if (managers.isEmpty()) {
            throw new IllegalStateException("Branch has no manager");
        }


        // LƯU NHIỀU SHIFT REQUEST
        List<ShiftRequest> saved = req.getShifts().stream().map(s -> {


            ShiftRequest sr = ShiftRequest.builder()
                    .account(employee)
                    .branch(branch)
                    .shiftDate(s.getDate())   // lấy từ shifts[]
                    .shiftType(s.getShiftType())
                    .note(req.getNote())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();


            return repo.save(sr);


        }).toList();


        // Gửi thông báo manager
        managers.forEach(m ->
                System.out.println("Notify Manager: " + m.getEmail())
        );


        // Trả về list response
        return saved.stream().map(mapper::toResponse).toList();
    }


    // Lấy theo branch
    public List<ShiftRequestResponse> getRequestsByBranch(Integer branchId) {
        return mapper.toResponseList(repo.findByBranch_Id(branchId));
    }


    // Lấy theo nhân viên
    public List<ShiftRequestResponse> getRequestsByAccount(Integer accountId) {
        return mapper.toResponseList(repo.findByAccount_AccountID(accountId));
    }


    // Duyệt / từ chối
    public ShiftRequestResponse updateStatus(Integer requestId, String status) {
        ShiftRequest sr = repo.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ShiftRequest not found: " + requestId));


        sr.setStatus(status.toUpperCase());
        ShiftRequest saved = repo.save(sr);


        // ⭐ Nếu Manager DUYỆT → tạo WorkSchedule
        if ("APPROVED".equalsIgnoreCase(status)) {


            // Kiểm tra nếu ca làm đã tồn tại
            boolean exists = workScheduleRepository
                    .existsByBranch_IdAndShiftDateAndShiftTypeAndAccount_AccountID(
                            sr.getBranch().getId(),
                            sr.getShiftDate(),
                            sr.getShiftType(),
                            sr.getAccount().getAccountID()
                    );


            if (!exists) {


                // Giờ làm mặc định
                LocalTime start = null;
                LocalTime end = null;


                switch (sr.getShiftType()) {
                    case "MORNING" -> { start = LocalTime.of(8, 0); end = LocalTime.of(13, 0); }
                    case "AFTERNOON" -> { start = LocalTime.of(13, 0); end = LocalTime.of(18, 0); }
                    case "NIGHT" -> { start = LocalTime.of(18, 0); end = LocalTime.of(23, 0); }
                }


                WorkSchedule ws = WorkSchedule.builder()
                        .branch(sr.getBranch())
                        .account(sr.getAccount())
                        .shiftDate(sr.getShiftDate())
                        .shiftType(sr.getShiftType())
                        .startTime(start)
                        .endTime(end)
                        .note(sr.getNote())
                        .build();


                workScheduleRepository.save(ws);
            }
        }


        return mapper.toResponse(saved);
    }




}

