package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.service.AuditoriumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoriums")
@RequiredArgsConstructor
public class AuditoriumController {

    private final AuditoriumService service;

    @GetMapping
    public ResponseEntity<List<AuditoriumResponse>> list(@RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(service.listByBranch(branchId));
    }
}
