package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ScreeningPeriodRequest;
import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.service.ScreeningPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/screening-periods")
@RequiredArgsConstructor
public class ScreeningPeriodController {

    private final ScreeningPeriodService screeningPeriodService;

    // POST: /api/v1/screening-periods
    @PostMapping
    public ResponseEntity<ScreeningPeriodResponse> createPeriod(@RequestBody ScreeningPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(screeningPeriodService.create(request));
    }

    // GET: /api/v1/screening-periods
    @GetMapping
    public ResponseEntity<List<ScreeningPeriodResponse>> getAllPeriods() {
        return ResponseEntity.ok(screeningPeriodService.getAll());
    }

    // GET: /api/v1/screening-periods/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ScreeningPeriodResponse> getPeriodById(@PathVariable Integer id) {
        return ResponseEntity.ok(screeningPeriodService.getById(id));
    }

    // GET: /api/v1/screening-periods/branch/{branchId} (Dùng cho lọc)
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ScreeningPeriodResponse>> getPeriodsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.ok(screeningPeriodService.getByBranchId(branchId));
    }

    // PUT: /api/v1/screening-periods/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ScreeningPeriodResponse> updatePeriod(@PathVariable Integer id, @RequestBody ScreeningPeriodRequest request) {
        return ResponseEntity.ok(screeningPeriodService.update(id, request));
    }

    // DELETE: /api/v1/screening-periods/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePeriod(@PathVariable Integer id) {
        screeningPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}