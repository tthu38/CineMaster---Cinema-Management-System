package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.*;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.mapper.WorkScheduleMapper;
import com.example.cinemaster.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    private final WorkScheduleRepository repo;
    private final AccountRepository accountRepo;
    private final BranchRepository branchRepo;
    private final WorkScheduleMapper mapper; // ✅ inject mapper bean

    public WorkScheduleResponse create(WorkScheduleCreateRequest req) {
        Account acc = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.getAccountId()));
        Branch br = branchRepo.findById(req.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + req.getBranchId()));

        ensureAccountBelongsToBranch(acc, br.getId());

        WorkSchedule e = mapper.toEntity(req, acc, br);
        return mapper.toResponse(repo.save(e));
    }

    public WorkScheduleResponse update(Integer id, WorkScheduleUpdateRequest req) {
        WorkSchedule e = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + id));
        mapper.updateEntity(req, e);
        return mapper.toResponse(repo.save(e));
    }

    public void delete(Integer id) {
        if (!repo.existsById(id))
            throw new EntityNotFoundException("Schedule not found: " + id);
        repo.deleteById(id);
    }

    public WorkScheduleResponse get(Integer id) {
        return repo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + id));
    }

    public PageResponse<WorkScheduleResponse> search(Integer accountId, Integer branchId,
                                                     LocalDate from, LocalDate to, Pageable pageable) {
        Specification<WorkSchedule> spec = Specification.allOf(
                WorkScheduleRepository.hasAccount(accountId),
                WorkScheduleRepository.hasBranch(branchId),
                WorkScheduleRepository.dateBetween(from, to)
        );
        Page<WorkSchedule> page = repo.findAll(spec, pageable);
        List<WorkScheduleResponse> items = page.stream()
                .map(mapper::toResponse)
                .toList();
        return new PageResponse<>(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public Map<String, Map<String, List<WorkScheduleCellAssignmentResponse>>> getMatrix(LocalDate from, LocalDate to, Integer branchId) {
        var spec = Specification.allOf(
                WorkScheduleRepository.hasBranch(branchId),
                WorkScheduleRepository.dateBetween(from, to)
        );

        List<WorkSchedule> rows = repo.findAll(spec);

        if (branchId != null) {
            rows = rows.stream()
                    .filter(ws -> {
                        Account a = ws.getAccount();
                        return a != null && a.getBranch() != null
                                && Objects.equals(a.getBranch().getId(), branchId);
                    })
                    .toList();
        }

        return mapper.toMatrix(rows);
    }

    public List<WorkScheduleCellAssignmentResponse> getCell(Integer branchId, LocalDate date, String shiftType) {
        return repo.findByBranch_IdAndShiftDateAndShiftType(branchId, date, shiftType).stream()
                .map(mapper::toCellAssignment)
                .toList();
    }

    @Transactional
    public WorkScheduleResponse upsertCellMany(WorkScheduleUpsertCellManyRequest req) {
        var br = branchRepo.findById(req.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + req.getBranchId()));

        repo.deleteCell(req.getBranchId(), req.getDate(), req.getShiftType());

        var defaults = Map.of(
                "MORNING", new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(12, 0)},
                "AFTERNOON", new LocalTime[]{LocalTime.of(13, 0), LocalTime.of(17, 0)},
                "NIGHT", new LocalTime[]{LocalTime.of(18, 0), LocalTime.of(22, 0)}
        );
        var times = defaults.getOrDefault(req.getShiftType(),
                new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(12, 0)});

        WorkSchedule last = null;

        if (req.getAccountIds() != null) {
            for (Integer accId : req.getAccountIds()) {
                var acc = accountRepo.findById(accId)
                        .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accId));

                ensureAccountBelongsToBranch(acc, req.getBranchId());

                WorkSchedule e = mapper.toEntityForUpsert(acc, br, req, times);
                last = repo.save(e);
            }
        }
        return last == null ? null : mapper.toResponse(last);
    }
    public void ensureScheduleInBranch(Integer scheduleId, Integer managerBranchId) {
        WorkSchedule ws = repo.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));

        Integer scheduleBranch = ws.getBranch().getId();
        if (!Objects.equals(scheduleBranch, managerBranchId)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa lịch của chi nhánh khác!");
        }
    }

    public void ensureAccountBelongsToBranch(Account acc, Integer branchId) {
        if (acc == null) {
            throw new IllegalArgumentException("Account is null");
        }
        Integer accBranch = acc.getBranch() != null ? acc.getBranch().getId() : null;
        if (!Objects.equals(accBranch, branchId)) {
            throw new IllegalArgumentException(
                    "Nhân viên #" + acc.getAccountID() + " không thuộc chi nhánh " + branchId
            );
        }
    }

    public void ensureAccountBelongsToBranchs(Integer accountId, Integer branchId) {
        Account acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        Integer accBranch = acc.getBranch() != null ? acc.getBranch().getId() : null;
        if (!Objects.equals(accBranch, branchId)) {
            throw new IllegalArgumentException("Nhân viên #" + accountId + " không thuộc chi nhánh #" + branchId);
        }
    }
    public boolean hasShift(Integer accountId, LocalDate date) {
        if (accountId == null || date == null) return false;
        return repo.existsByAccount_AccountIDAndShiftDate(accountId, date);
    }

}
