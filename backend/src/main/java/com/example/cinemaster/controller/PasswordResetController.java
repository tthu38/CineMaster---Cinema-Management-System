package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.ResetPasswordRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.RoleRepository;
import com.example.cinemaster.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    //huyền counterpassword
    @Autowired
    private RoleRepository roleRepository;
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<String>> inviteCustomer(@RequestBody Map<String, String> body) {
        ApiResponse<String> response = new ApiResponse<>();

        String email = body.get("email");
        String phone = body.get("phone");

        // Check email trùng
        if (accountRepository.existsByEmail(email)) {
            response.setCode(4001);
            response.setMessage("Email đã tồn tại trong hệ thống");
            return ResponseEntity.badRequest().body(response);
        }

        // Check số điện thoại trùng
        if (accountRepository.existsByPhoneNumber(phone)) {
            response.setCode(4002);
            response.setMessage("Số điện thoại đã tồn tại trong hệ thống");
            return ResponseEntity.badRequest().body(response);
        }

        Account acc = new Account();
        acc.setEmail(email);
        acc.setPhoneNumber(phone);
        acc.setIsActive(false);
        acc.setCreatedAt(LocalDate.now());

        // 👇 Tự động đặt username theo prefix email
        String username = email.substring(0, email.indexOf('@'));
        acc.setFullName(username); // hoặc acc.setUsername(username) nếu bạn có cột Username riêng

        // Role mặc định là Customer
        Role role = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role"));
        acc.setRole(role);

        // Token đặt mật khẩu
        String token = UUID.randomUUID().toString();
        acc.setPasswordResetToken(token);
        acc.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));

        accountRepository.save(acc);

        try {
            emailService.sendInviteEmail(acc.getEmail(), token);
            response.setCode(1000);
            response.setMessage("Đã gửi link đặt mật khẩu tới email: " + acc.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(9999);
            response.setMessage("Không gửi được email: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }





    @PostMapping("/reset-by-token")
    public ResponseEntity<ApiResponse<String>> resetByToken(@RequestBody Map<String, String> body) {
        ApiResponse<String> response = new ApiResponse<>();
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || newPassword == null) {
            response.setCode(4000);
            response.setMessage("Thiếu token hoặc mật khẩu mới");
            return ResponseEntity.badRequest().body(response);
        }

        Account acc = accountRepository.findAll()
                .stream()
                .filter(a -> token.equals(a.getPasswordResetToken()))
                .findFirst()
                .orElse(null);

        if (acc == null || acc.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            response.setCode(4004);
            response.setMessage("Token không hợp lệ hoặc đã hết hạn");
            return ResponseEntity.badRequest().body(response);
        }

        acc.setPassword(passwordEncoder.encode(newPassword));
        acc.setPasswordResetToken(null);
        acc.setPasswordResetTokenExpiry(null);
        acc.setIsActive(true);
        accountRepository.save(acc);

        response.setCode(1000);
        response.setMessage("Đặt mật khẩu thành công!");
        return ResponseEntity.ok(response);
    }


}