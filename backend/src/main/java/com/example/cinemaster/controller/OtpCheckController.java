package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.OtpCheckRequest;
import com.example.cinemaster.dto.response.OtpCheckResponse;
import com.example.cinemaster.service.OtpCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpCheckController {
    private final OtpCheckService otpCheckService;

    @PostMapping("/check")
    public ResponseEntity<OtpCheckResponse> checkOtp(@RequestBody OtpCheckRequest request) {
        return ResponseEntity.ok(otpCheckService.checkOtp(request));
    }
}