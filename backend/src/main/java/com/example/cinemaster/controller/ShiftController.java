package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ShiftReportResponse;
import com.example.cinemaster.dto.response.ShiftSessionResponse;
import com.example.cinemaster.entity.ShiftSession;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 🎬 ShiftController
 * Quản lý ca làm việc của nhân viên (Staff)
 * - Mở ca
 * - Báo cáo doanh thu ca hiện tại
 * - Kết ca
 *
 * Yêu cầu:
 *  - Người dùng phải đăng nhập và có role STAFF.
 *  - Sử dụng JWT Authentication (AccountPrincipal được inject tự động).
 */
@RestController
@RequestMapping("/api/v1/staff/shift")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    /* ============================================================
       🟢 MỞ CA
    ============================================================ */
    @PreAuthorize("hasRole('Staff')")
    @PostMapping("/open")
    public ResponseEntity<ShiftSession> openShift(
            @AuthenticationPrincipal AccountPrincipal user,
            @RequestParam BigDecimal openingCash
    ) {
        System.out.println("🟢 Mở ca - user: " + user.getFullName() + " | role=" + user.getRole());
        ShiftSession session = shiftService.openShift(user, openingCash);
        return ResponseEntity.ok(session);
    }

    /* ============================================================
       📊 BÁO CÁO DOANH THU CA HIỆN TẠI
    ============================================================ */
    @PreAuthorize("hasRole('Staff')")
    @GetMapping("/report")
    public ResponseEntity<ShiftReportResponse> getReport(
            @AuthenticationPrincipal AccountPrincipal user
    ) {
        System.out.println("📊 Báo cáo ca - user: " + user.getFullName() + " | role=" + user.getRole());
        ShiftReportResponse report = shiftService.getCurrentShiftReport(user);
        return ResponseEntity.ok(report);
    }

    /* ============================================================
       🔴 KẾT CA
    ============================================================ */
    @PreAuthorize("hasRole('Staff')")
    @PostMapping("/close")
    public ResponseEntity<ShiftSessionResponse> closeShift(
            @AuthenticationPrincipal AccountPrincipal user,
            @RequestParam BigDecimal closingCash
    ) {
        System.out.println("🔴 Kết ca - user: " + user.getFullName() + " | role=" + user.getRole());
        ShiftSession session = shiftService.closeShift(user, closingCash);

        ShiftSessionResponse response = ShiftSessionResponse.builder()
                .id(session.getId())
                .staffId(user.getId())
                .staffName(user.getFullName())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .openingCash(session.getOpeningCash())
                .closingCash(session.getClosingCash())
                .expectedCash(session.getExpectedCash())
                .difference(session.getDifference())
                .status(session.getStatus())
                .build();

        return ResponseEntity.ok(response);
    }

}
