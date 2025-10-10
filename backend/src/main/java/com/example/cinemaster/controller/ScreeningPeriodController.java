package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.mapper.ScreeningPeriodMapper;
import com.example.cinemaster.service.ScreeningPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/screening-periods")
@RequiredArgsConstructor
public class ScreeningPeriodController {

    private final ScreeningPeriodService service;      // <- final để Lombok inject
    private final ScreeningPeriodMapper mapper;        // <- inject mapper bean

    @GetMapping("/active")
    public ResponseEntity<List<ScreeningPeriodResponse>> active(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate
    ){
        var date = (onDate != null) ? onDate : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        var list = service.findActive(branchId, date);
        return ResponseEntity.ok(mapper.toLiteList(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningPeriodResponse> getById(@PathVariable Integer id){
        var sp = service.getById(id);
        return ResponseEntity.ok(mapper.toLite(sp));
    }
}
