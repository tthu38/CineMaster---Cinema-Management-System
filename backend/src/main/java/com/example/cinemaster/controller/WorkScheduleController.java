package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.*;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.WorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/work-schedules")
public class WorkScheduleController {

    private final WorkScheduleService service;

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping
    public ResponseEntity<WorkScheduleResponse> create(
            @Valid @RequestBody WorkScheduleCreateRequest req,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // Nếu là Manager → bắt buộc dùng branch của họ
        if (user.hasRole("Manager")) {
            req.setBranchId(user.getBranchId());
        }

        // ✅ Kiểm tra: nhân viên được gán phải thuộc chi nhánh này
        service.ensureAccountBelongsToBranchs(req.getAccountId(), req.getBranchId());

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }


    /* ======================= UPDATE ======================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<WorkScheduleResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody WorkScheduleUpdateRequest req,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // Nếu là Manager → chỉ được sửa trong chi nhánh của họ
        if (user.hasRole("Manager")) {
            service.ensureScheduleInBranch(id, user.getBranchId());
        }

        return ResponseEntity.ok(service.update(id, req));
    }

    /* ======================= DELETE ======================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // Nếu là Manager → chỉ xóa lịch trong chi nhánh của họ
        if (user.hasRole("Manager")) {
            service.ensureScheduleInBranch(id, user.getBranchId());
        }

        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /* ======================= READ ======================= */
    @GetMapping("/{id}")
    public ResponseEntity<WorkScheduleResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkScheduleResponse>> search(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "shiftDate,ASC") String sort
    ) {
        String[] s = sort.split(",");
        Sort sortObj = (s.length == 2)
                ? Sort.by(Sort.Direction.fromString(s[1]), s[0])
                : Sort.by("shiftDate").ascending();
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(service.search(accountId, branchId, from, to, pageable));
    }

    /* ======================= MATRIX & CELL ======================= */
    @GetMapping("/matrix")
    public ResponseEntity<Map<String, Map<String, List<WorkScheduleCellAssignmentResponse>>>> matrix(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam Integer branchId
    ) {
        return ResponseEntity.ok(service.getMatrix(from, to, branchId));
    }

    @GetMapping("/cell")
    public ResponseEntity<List<WorkScheduleCellAssignmentResponse>> getCell(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String shiftType
    ) {
        return ResponseEntity.ok(service.getCell(branchId, date, shiftType));
    }

    /* ======================= UPSERT CELL MANY ======================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/upsert-cell-many")
    public ResponseEntity<WorkScheduleResponse> upsertCellMany(
            @Valid @RequestBody WorkScheduleUpsertCellManyRequest req,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // Nếu là Manager → chỉ được upsert cell trong chi nhánh của mình
        if (user.hasRole("Manager")) {
            if (!user.getBranchId().equals(req.getBranchId())) {
                throw new IllegalArgumentException("Không được thao tác lịch của chi nhánh khác.");
            }
        }

        return ResponseEntity.ok(service.upsertCellMany(req));
    }
}
