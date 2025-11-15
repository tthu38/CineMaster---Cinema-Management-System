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
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {


    private final ShowtimeService service;

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping
    public ResponseEntity<ShowtimeResponse> create(
            @Valid @RequestBody ShowtimeCreateRequest request,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        ShowtimeResponse response = service.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ShowtimeResponse>> search(
            @RequestParam(required = false) Integer periodId,
            @RequestParam(required = false) Integer auditoriumId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime,asc") String sort
    ) {
        Sort sortSpec = sort.toLowerCase().endsWith(",desc")
                ? Sort.by(sort.split(",")[0]).descending()
                : Sort.by(sort.split(",")[0]).ascending();


        Pageable pageable = PageRequest.of(page, size, sortSpec);


        LocalDateTime fromTime = null, toTime = null;
        try {
            if (from != null && !from.isEmpty()) {
                if (from.contains("T")) fromTime = LocalDateTime.parse(from);
                else fromTime = LocalDate.parse(from).atStartOfDay();
            }
            if (to != null && !to.isEmpty()) {
                if (to.contains("T")) toTime = LocalDateTime.parse(to);
                else toTime = LocalDate.parse(to).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày/giờ không hợp lệ. " +
                    "Dùng yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss");
        }


        Page<ShowtimeResponse> result = service.search(periodId, auditoriumId, fromTime, toTime, pageable);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody ShowtimeUpdateRequest request,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        ShowtimeResponse response = service.update(id, request, user);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/next-week")
    public ResponseEntity<List<DayScheduleResponse>> nextWeek(
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(service.getNextWeekSchedule(branchId));
    }

    @GetMapping("/week")
    public ResponseEntity<List<DayScheduleResponse>> week(
            @RequestParam(required = false) String anchor,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer movieId, // 🆕 Thêm lọc phim
            Authentication auth
    ) {
        LocalDate anchorDate = null;
        try {
            if (anchor != null && !anchor.isEmpty()) {
                anchorDate = LocalDate.parse(anchor);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng anchor không hợp lệ (yyyy-MM-dd).");
        }


        AccountPrincipal user = (auth != null && auth.getPrincipal() instanceof AccountPrincipal)
                ? (AccountPrincipal) auth.getPrincipal()
                : null;

        Integer effectiveBranchId = branchId;
        if (user != null && user.isManager()) {
            effectiveBranchId = user.getBranchId();
        }
        LocalDate targetWeek = (anchorDate != null ? anchorDate : LocalDate.now()).plusWeeks(offset);

        return ResponseEntity.ok(service.getWeekSchedule(targetWeek, effectiveBranchId, movieId));
    }


    @GetMapping("/next7days")
    public ResponseEntity<List<DayScheduleResponse>> next7days(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer movieId
    ) {
        return ResponseEntity.ok(service.getNext7DaysSchedule(branchId, movieId));
    }


}

