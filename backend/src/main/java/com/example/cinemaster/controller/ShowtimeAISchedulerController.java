package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ShowtimeService;
import com.example.cinemaster.service.ShowtimeAISchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ai/scheduler")
@RequiredArgsConstructor
public class ShowtimeAISchedulerController {

    private final ShowtimeAISchedulerService aiService;
    private final ShowtimeService showtimeService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var result = aiService.generateSchedule(branchId, date);
        return ResponseEntity.ok(result);
    }

    /**
     * 💾 Lưu danh sách lịch chiếu do AI gợi ý vào database
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveSchedules(
            @RequestBody List<ShowtimeCreateRequest> showtimes,
            @AuthenticationPrincipal AccountPrincipal user) {

        List<ShowtimeResponse> saved = showtimes.stream()
                .map(req -> showtimeService.create(req, user))
                .toList();

        return ResponseEntity.ok(saved);
    }
}
