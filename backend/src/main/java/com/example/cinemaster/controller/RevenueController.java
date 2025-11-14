package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.RevenueQueryResquest;
import com.example.cinemaster.dto.request.RevenueScopeResquest;
import com.example.cinemaster.dto.response.RevenueDayResponse;
import com.example.cinemaster.dto.response.RevenueRowResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService service;

    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/daily")
    public ResponseEntity<List<RevenueDayResponse>> getDailyRevenue(Authentication auth) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(service.getRevenue(user));
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/by-day")
    public ResponseEntity<List<RevenueRowResponse>> getRevenueByDay(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(name = "anchorDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate,
            @RequestParam(required = false) Integer branchId
    ) {
        LocalDate safeDate = (anchorDate != null) ? anchorDate : LocalDate.now();

        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.DAY)
                .anchorDate(safeDate)
                .branchId(branchId)
                .build();

        return ResponseEntity.ok(service.getReport(q, principal));
    }


    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/by-month")
    public ResponseEntity<List<RevenueRowResponse>> byMonth(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer branchId,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.MONTH)
                .year(year)
                .branchId(branchId)
                .build();
        return ResponseEntity.ok(service.getReport(q, user));
    }

    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/by-year")
    public ResponseEntity<List<RevenueRowResponse>> byYear(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer branchId,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.YEAR)
                .fromYear(fromYear)
                .toYear(toYear)
                .branchId(branchId)
                .build();
        return ResponseEntity.ok(service.getReport(q, user));
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/last7days")
    public ResponseEntity<?> getLast7DaysRevenue(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(required = false) Integer branchId
    ) {
        try {
            return ResponseEntity.ok(service.getRevenueLast7Days(principal, branchId));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Không quyền truy cập chức năng này.",
                    "detail", se.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Lỗi server",
                    "detail", e.getMessage()
            ));
        }
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/by-month-detail")
    public ResponseEntity<?> getRevenueByMonthDetail(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(service.getReport(
                RevenueQueryResquest.builder()
                        .scope(RevenueScopeResquest.MONTH)
                        .year(year)
                        .branchId(branchId)
                        .build(),
                principal
        ));

    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/custom-range")
    public ResponseEntity<?> getRevenueCustomRange(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer branchId
    ) {
        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.CUSTOM)
                .from(from)
                .to(to)
                .branchId(branchId)
                .build();

        return ResponseEntity.ok(service.getReport(q, principal));
    }


    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/top-movies")
    public ResponseEntity<?> getTopMovies(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(service.getTop10Movies(principal, branchId, from, to, year, month));
    }
}
