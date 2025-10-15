package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.DayScheduleResponse;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService service;

    // ========= CREATE =========
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping
    public ResponseEntity<ShowtimeResponse> create(
            @Valid @RequestBody ShowtimeCreateRequest request,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        ShowtimeResponse response = service.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========= GET BY ID =========
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // ========= SEARCH =========
    @GetMapping
    public ResponseEntity<Page<ShowtimeResponse>> search(
            @RequestParam(required = false) Integer periodId,
            @RequestParam(required = false) Integer auditoriumId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime,asc") String sort
    ) {
        Sort sortSpec = sort.toLowerCase().endsWith(",desc")
                ? Sort.by(sort.split(",")[0]).descending()
                : Sort.by(sort.split(",")[0]).ascending();

        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ResponseEntity.ok(service.search(periodId, auditoriumId, from, to, pageable));
    }

    // ========= UPDATE =========
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody ShowtimeUpdateRequest request,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        ShowtimeResponse response = service.update(id, request, user);
        return ResponseEntity.ok(response);
    }

    // ========= DELETE =========
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication auth) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    // ========= WEEKLY SCHEDULE =========
    @GetMapping("/next-week")
    public ResponseEntity<List<DayScheduleResponse>> nextWeek(
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(service.getNextWeekSchedule(branchId));
    }

    @GetMapping("/week")
    public ResponseEntity<List<DayScheduleResponse>> week(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchor,
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(service.getWeekSchedule(anchor, branchId));
    }
}
