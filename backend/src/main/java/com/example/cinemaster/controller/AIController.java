package com.example.cinemaster.controller;


import com.example.cinemaster.dto.request.AiPreviewSaveRequest;
import com.example.cinemaster.dto.response.AiPreviewResponse;
import com.example.cinemaster.service.ai.AISchedulerService;


import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;


@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {


    private final AISchedulerService aiSchedulerService;


    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/preview")
    public ResponseEntity<AiPreviewResponse> previewSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        return ResponseEntity.ok(aiSchedulerService.generatePreviewSchedule(branchId, weekStart));
    }


    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping("/save")
    public ResponseEntity<?> saveSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart,
            @RequestBody AiPreviewSaveRequest req
    ) {
        aiSchedulerService.saveGeneratedSchedule(branchId, weekStart, req);
        return ResponseEntity.ok().body("Saved");
    }


    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/schedule")
    public ResponseEntity<AiPreviewResponse> getSchedule(
            @RequestParam Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        return ResponseEntity.ok(aiSchedulerService.getGeneratedSchedule(branchId, weekStart));
    }
}

