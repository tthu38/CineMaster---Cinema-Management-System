package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ShowtimeAISchedulerService;
import com.example.cinemaster.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
public class ShowtimeAISchedulerController {

    private final ShowtimeAISchedulerService aiService;
    private final ShowtimeService showtimeService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<?> generateSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var result = aiService.generateSchedule(branchId, date);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<?> approveSchedules(
            @RequestBody List<ShowtimeCreateRequest> showtimes,
            @AuthenticationPrincipal AccountPrincipal user) {

        List<ShowtimeResponse> saved = showtimes.stream()
                .map(req -> showtimeService.create(req, user))
                .toList();

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/approve/public")
    public ResponseEntity<?> approveSchedulesPublic(
            @RequestBody List<ShowtimeCreateRequest> showtimes) {
        List<ShowtimeResponse> saved = showtimes.stream()
                .map(req -> showtimeService.createFromAI(req))
                .toList();
        return ResponseEntity.ok(saved);
    }
}
