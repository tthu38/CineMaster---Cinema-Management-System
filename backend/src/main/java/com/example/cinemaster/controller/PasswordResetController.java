package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.ResetPasswordRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<String>> requestOtp(@RequestBody EmailRequest request) {
        ApiResponse<String> response = new ApiResponse<>();

        Account acc = accountRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (acc == null) {
            response.setCode(4001);
            response.setMessage("Email không tồn tại trong hệ thống");
            return ResponseEntity.badRequest().body(response);
        }

        // Sinh OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        acc.setVerificationCode(otp);
        acc.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));
        accountRepository.save(acc);

        try {
            emailService.sendVerificationEmail(acc.getEmail(), otp);
            response.setCode(1000);
            response.setMessage("OTP đã gửi về email: " + acc.getEmail());
            response.setResult("OK");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Send OTP error: {}", e.getMessage());
            response.setCode(9999);
            response.setMessage("Không gửi được OTP: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse<String> response = new ApiResponse<>();

        Account acc = accountRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (acc == null) {
            response.setCode(4001);
            response.setMessage("Email không tồn tại trong hệ thống");
            return ResponseEntity.badRequest().body(response);
        }

        // Check OTP
        if (acc.getVerificationCode() == null
                || !acc.getVerificationCode().equals(request.getOtp())) {
            response.setCode(4002);
            response.setMessage("OTP không chính xác");
            return ResponseEntity.badRequest().body(response);
        }

        if (acc.getVerificationExpiry() == null
                || acc.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            response.setCode(4003);
            response.setMessage("OTP đã hết hạn");
            return ResponseEntity.badRequest().body(response);
        }

        // Đặt lại mật khẩu
        acc.setPassword(passwordEncoder.encode(request.getNewPassword()));
        acc.setVerificationCode(null);
        acc.setVerificationExpiry(null);
        accountRepository.save(acc);

        response.setCode(1000);
        response.setMessage("Đặt lại mật khẩu thành công!");
        response.setResult("SUCCESS");
        return ResponseEntity.ok(response);
    }
}
