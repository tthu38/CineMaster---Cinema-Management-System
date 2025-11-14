package com.example.cinemaster.controller;


import com.example.cinemaster.entity.WorkSchedule;
import com.example.cinemaster.service.ai.AISchedulerService;


import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {


    private final AISchedulerService aiSchedulerService;


    /**
     * Chạy AI để sinh lịch làm cho 1 tuần
     * Ví dụ FE gọi:
     * POST /api/v1/ai/schedule?branchId=4&weekStart=2025-11-17
     */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping("/schedule")
    public ResponseEntity<List<WorkSchedule>> generateSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        List<WorkSchedule> result = aiSchedulerService.generateWeeklySchedule(branchId, weekStart);
        return ResponseEntity.ok(result);
    }


    /**
     * Xem lịch đã được AI sinh ra trong tuần
     */
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/schedule")
    public ResponseEntity<List<WorkSchedule>> getSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        List<WorkSchedule> list = aiSchedulerService.getGeneratedSchedule(branchId, weekStart);
        return ResponseEntity.ok(list);
    }
}



