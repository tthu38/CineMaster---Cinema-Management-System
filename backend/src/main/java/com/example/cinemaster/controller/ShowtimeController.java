// src/main/java/com/example/cinemaster/controller/ShowtimeController.java
package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.DayScheduleResponse;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {

    private final ShowtimeService service;

    public ShowtimeController(ShowtimeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> create(@Valid @RequestBody ShowtimeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ShowtimeResponse>> search(
            @RequestParam(required = false) Integer periodId,
            @RequestParam(required = false) Integer auditoriumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime,asc") String sort
    ) {
        Sort sortSpec = Sort.by(sort.split(",")[0]).ascending();
        if (sort.toLowerCase().endsWith(",desc")) sortSpec = sortSpec.descending();

        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ResponseEntity.ok(service.search(periodId, auditoriumId, from, to, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody ShowtimeUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    // ShowtimeController.java (thêm)
    @GetMapping("/next-week")
    public ResponseEntity<List<DayScheduleResponse>> nextWeek(
            @RequestParam(required = false) Integer branchId
    ){
        return ResponseEntity.ok(service.getNextWeekSchedule(branchId));
    }
    @GetMapping("/week")
    public ResponseEntity<List<DayScheduleResponse>> week(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchor,
            @RequestParam(required = false) Integer branchId
    ){
        // anchor = null -> service sẽ mặc định dùng LocalDate.now()
        return ResponseEntity.ok(service.getWeekSchedule(anchor, branchId));
    }

}
