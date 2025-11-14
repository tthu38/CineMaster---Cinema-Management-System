package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ScreeningPeriodRequest;
import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.mapper.ScreeningPeriodMapper;
import com.example.cinemaster.service.ScreeningPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/screening-periods")
@RequiredArgsConstructor
public class ScreeningPeriodController {

    private final ScreeningPeriodService screeningPeriodService;
    private final ScreeningPeriodMapper screeningPeriodMapper;

    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<ScreeningPeriodResponse> createPeriod(@RequestBody ScreeningPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(screeningPeriodService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ScreeningPeriodResponse>> getAllPeriods() {
        return ResponseEntity.ok(screeningPeriodService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningPeriodResponse> getPeriodById(@PathVariable Integer id) {
        return ResponseEntity.ok(screeningPeriodService.getById(id));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ScreeningPeriodResponse>> getPeriodsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.ok(screeningPeriodService.getByBranchId(branchId));
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<ScreeningPeriodResponse> updatePeriod(@PathVariable Integer id, @RequestBody ScreeningPeriodRequest request) {
        return ResponseEntity.ok(screeningPeriodService.update(id, request));
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePeriod(@PathVariable Integer id) {
        screeningPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<ScreeningPeriodResponse>> active(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate
    ) {
        var date = (onDate != null) ? onDate : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        var list = screeningPeriodService.findActive(branchId, date);
        return ResponseEntity.ok(screeningPeriodMapper.toLiteList(list));
    }
    @GetMapping("/search")
    public ResponseEntity<List<ScreeningPeriodResponse>> searchByMovieTitle(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(screeningPeriodService.searchByMovieTitle(keyword));
    }

}
