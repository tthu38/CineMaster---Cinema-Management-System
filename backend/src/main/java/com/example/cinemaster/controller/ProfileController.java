package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ChangePasswordRequest;
import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.service.EmailService;
import com.example.cinemaster.service.JwtService;
import com.example.cinemaster.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class ProfileController {

    private final JwtService jwtService;
    private final JwtUtil jwtUtil;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal Account account,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (account == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<ProfileResponse>builder()
                            .code(401)
                            .message("Unauthorized: No user found in context")
                            .build()
            );
        }

        // Lấy roleName từ JWT (claim "role")
        String jwt = authHeader.substring(7);
        String roleName = jwtService.extractRole(jwt);

        ProfileResponse profileResponse = ProfileResponse.builder()
                .id(account.getAccountID())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phoneNumber(account.getPhoneNumber())
                .address(account.getAddress())
                .roleName(roleName)   // 👈 lấy từ token, không từ entity
                .createdAt(account.getCreatedAt())
                .avatarUrl(account.getAvatarUrl())
                .loyaltyPoints(account.getLoyaltyPoints())
                .build();

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .code(200)
                        .message("Success")
                        .result(profileResponse)
                        .build()
        );
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody @Validated UpdateProfileRequest updateRequest,
            @AuthenticationPrincipal Account account) {

        ApiResponse<ProfileResponse> apiResponse = new ApiResponse<>();

        if (account == null) {
            apiResponse.setCode(401);
            apiResponse.setMessage("Unauthorized");
            return ResponseEntity.status(401).body(apiResponse);
        }

        try {
            boolean emailChanged = false;

            // Update fullname nếu có
            if (updateRequest.getFullName() != null && !updateRequest.getFullName().isBlank()) {
                account.setFullName(updateRequest.getFullName());
            }

            // Update phoneNumber nếu có
            if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().isBlank()) {
                account.setPhoneNumber(updateRequest.getPhoneNumber());
            }

            // Update address nếu có
            if (updateRequest.getAddress() != null && !updateRequest.getAddress().isBlank()) {
                account.setAddress(updateRequest.getAddress());
            }

            // Nếu email thay đổi thì gửi OTP
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()
                    && !account.getEmail().equals(updateRequest.getEmail())) {

                // Sinh OTP ngẫu nhiên 6 số
                String code = String.valueOf((int) (Math.random() * 900000) + 100000);

                // Lưu OTP + expiry vào account
                account.setVerificationCode(code);
                account.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));

                // Gửi email OTP
                emailService.sendVerificationEmail(updateRequest.getEmail(), code);

                emailChanged = true;
            }

            // 🔹 Lưu cập nhật nếu không đổi email
            if (!emailChanged) {
                accountRepository.save(account);
            }

            // Build response profile
            ProfileResponse profileResponse = ProfileResponse.builder()
                    .id(account.getAccountID())
                    .email(account.getEmail())
                    .fullName(account.getFullName())
                    .phoneNumber(account.getPhoneNumber())
                    .address(account.getAddress())
                    .avatarUrl(account.getAvatarUrl())
                    .loyaltyPoints(account.getLoyaltyPoints())
                    .createdAt(account.getCreatedAt())
                    .build();

            if (emailChanged) {
                apiResponse.setCode(2001);
                apiResponse.setMessage("Please verify your new email address. We have sent a verification code to your new email.");
            } else {
                apiResponse.setCode(1000);
                apiResponse.setMessage("Profile updated successfully");
            }
            apiResponse.setResult(profileResponse);

            return ResponseEntity.ok(apiResponse);

        } catch (AppException ex) {
            log.error("Error updating profile: {}", ex.getMessage());
            apiResponse.setCode(ex.getErrorCode().getCode());
            apiResponse.setMessage(ex.getMessage());
            return ResponseEntity.status(400).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error updating profile: {}", e.getMessage(), e);
            apiResponse.setCode(9999);
            apiResponse.setMessage("Error updating user profile");
            return ResponseEntity.status(500).body(apiResponse);
        }
    }



    @PostMapping("/profile/send-otp-change-email")
    @Transactional
    public ResponseEntity<ApiResponse<String>> sendOtpChangeEmail(
            @RequestBody EmailRequest request,
            @AuthenticationPrincipal Account account) {
        ApiResponse<String> response = new ApiResponse<>();

        if (account == null) {
            response.setCode(401);
            response.setMessage("Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        // Check email đã tồn tại chưa
        if (accountRepository.existsByEmail(request.getEmail())) {
            response.setCode(2003);
            response.setMessage("Email already exists");
            return ResponseEntity.badRequest().body(response);
        }

        // Sinh OTP ngẫu nhiên
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Lưu OTP + expiry vào account
        account.setVerificationCode(code);
        account.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);

        // Gửi email
        try {
            emailService.sendVerificationEmail(request.getEmail(), code);
            response.setCode(1000);
            response.setMessage("OTP sent to new email successfully");
            response.setResult("OK");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send OTP to email {}: {}", request.getEmail(), e.getMessage());
            response.setCode(9999);
            response.setMessage("Failed to send OTP: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/profile/verify-email-change")
    @Transactional
    public ResponseEntity<ApiResponse<String>> verifyEmailChange(
            @RequestBody EmailRequest request,
            @AuthenticationPrincipal Account account) {
        ApiResponse<String> response = new ApiResponse<>();

        if (account == null) {
            response.setCode(401);
            response.setMessage("Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        // Check OTP
        if (account.getVerificationCode() == null
                || !account.getVerificationCode().equals(request.getOtp())) {
            response.setCode(2002);
            response.setMessage("Invalid verification code");
            return ResponseEntity.badRequest().body(response);
        }

        if (account.getVerificationExpiry() == null
                || account.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
            response.setCode(2002);
            response.setMessage("Verification code expired");
            return ResponseEntity.badRequest().body(response);
        }

        // Cập nhật email
        account.setEmail(request.getEmail());
        account.setVerificationCode(null);
        account.setVerificationExpiry(null);
        accountRepository.save(account);

        response.setCode(1000);
        response.setMessage("Email updated and verified successfully");
        response.setResult("SUCCESS");

        return ResponseEntity.ok(response);
    }
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Validated ChangePasswordRequest request,
            @AuthenticationPrincipal Account account
    ) {
        ApiResponse<String> apiResponse = new ApiResponse<>();

        if (account == null) {
            apiResponse.setCode(401);
            apiResponse.setMessage("Unauthorized");
            return ResponseEntity.status(401).body(apiResponse);
        }

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            apiResponse.setCode(2004);
            apiResponse.setMessage("Current password is incorrect");
            return ResponseEntity.badRequest().body(apiResponse);
        }

        // Đổi mật khẩu
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        apiResponse.setCode(1000);
        apiResponse.setMessage("Password changed successfully");
        apiResponse.setResult("OK");

        return ResponseEntity.ok(apiResponse);
    }




}





