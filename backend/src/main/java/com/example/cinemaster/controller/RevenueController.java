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
        List<RevenueDayResponse> data = service.getRevenue(user);
        return ResponseEntity.ok(data);
    }
    @GetMapping("/by-shift")
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    public ResponseEntity<List<RevenueRowResponse>> byShift(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer branchId,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.SHIFT)
                .anchorDate(date)
                .branchId(branchId)
                .build();
        return ResponseEntity.ok(service.getReport(q, user));
    }


    @GetMapping("/by-day")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<List<RevenueRowResponse>> byDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthAnchor,
            @RequestParam(required = false) Integer branchId,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        RevenueQueryResquest q = RevenueQueryResquest.builder()
                .scope(RevenueScopeResquest.DAY)
                .anchorDate(monthAnchor)
                .branchId(branchId)
                .build();
        return ResponseEntity.ok(service.getReport(q, user));
    }


    @GetMapping("/by-month")
    @PreAuthorize("hasRole('Admin')")
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


    @GetMapping("/by-year")
    @PreAuthorize("hasRole('Admin')")
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
            List<Map<String, Object>> data = service.getRevenueLast7Days(principal, branchId);
            return ResponseEntity.ok(data);
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
        return ResponseEntity.ok(service.getRevenueByMonth(principal, year, month, branchId));
    }


    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/custom-range")
    public ResponseEntity<?> getRevenueCustomRange(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(service.getRevenueBetweenDates(principal, from, to, branchId));
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

