package com.example.cinemaster.controller;


import com.example.cinemaster.dto.request.ChangePasswordRequest;
import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import jakarta.transaction.Transactional;
import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class ProfileController {


    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;


    /* =================== GET PROFILE ================= */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<ProfileResponse>builder()
                            .code(401)
                            .message("Không tìm thấy người dùng")
                            .build()
            );
        }


        ProfileResponse profile = ProfileResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .phoneNumber(principal.getPhoneNumber())
                .address(principal.getAddress())
                .roleName(principal.getRole())
                .createdAt(principal.getCreatedAt())
                .loyaltyPoints(principal.getLoyaltyPoints())
                .avatarUrl(principal.getAvatarUrl())
                .build();


        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .code(200)
                        .message("Thành công!")
                        .result(profile)
                        .build()
        );
    }


    /* ====================== UPDATE PROFILE========================= */
    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody @Validated UpdateProfileRequest req,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<ProfileResponse>builder()
                            .code(401)
                            .message("Unauthorized")
                            .build()
            );
        }


        var acc = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        if (req.getFullName() != null && !req.getFullName().isBlank())
            acc.setFullName(req.getFullName());
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank())
            acc.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null && !req.getAddress().isBlank())
            acc.setAddress(req.getAddress());


        accountRepository.save(acc);


        ProfileResponse updated = ProfileResponse.builder()
                .id(acc.getAccountID())
                .email(acc.getEmail())
                .fullName(acc.getFullName())
                .phoneNumber(acc.getPhoneNumber())
                .address(acc.getAddress())
                .roleName(principal.getRole())
                .createdAt(acc.getCreatedAt())
                .loyaltyPoints(acc.getLoyaltyPoints())
                .avatarUrl(acc.getAvatarUrl())
                .build();


        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .code(1000)
                        .message("Cập nhật thành công!")
                        .result(updated)
                        .build()
        );
    }


    /* ===================CHANGE PASSWORD======================== */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Validated ChangePasswordRequest req,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<String>builder()
                            .code(401)
                            .message("Unauthorized")
                            .build()
            );
        }


        var acc = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        if (!passwordEncoder.matches(req.getCurrentPassword(), acc.getPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .code(2004)
                            .message("Mật khẩu hiện tại sai!")
                            .build()
            );
        }


        acc.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(acc);


        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Thay đổi mật khẩu thành công")
                        .result("OK")
                        .build()
        );
    }


    /* ==================SEND OTP TO CHANGE EMAIL========================== */
    @PostMapping("/profile/send-otp-change-email")
    @Transactional
    public ResponseEntity<ApiResponse<String>> sendOtpChangeEmail(
            @RequestBody EmailRequest req,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<String>builder()
                            .code(401)
                            .message("Unauthorized")
                            .build()
            );
        }


        var acc = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        if (accountRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .code(2003)
                            .message("Email đã tồn tại!")
                            .build()
            );
        }


        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        acc.setVerificationCode(code);
        acc.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));
        accountRepository.save(acc);


        try {
            emailService.sendVerificationEmail(req.getEmail(), code);


            return ResponseEntity.ok(
                    ApiResponse.<String>builder()
                            .code(1000)
                            .message("Đã gửi OTP thành công")
                            .result("OK")
                            .build()
            );


        } catch (jakarta.mail.MessagingException e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<String>builder()
                            .code(9999)
                            .message("Failed to send OTP: " + e.getMessage())
                            .build()
            );
        }
    }


    /* =========================== VERIFY EMAIL CHANGE=========================================== */
    @PostMapping("/profile/verify-email-change")
    @Transactional
    public ResponseEntity<ApiResponse<String>> verifyEmailChange(
            @RequestBody EmailRequest req,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<String>builder()
                            .code(401)
                            .message("Unauthorized")
                            .build()
            );
        }


        var acc = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        if (acc.getVerificationCode() == null ||
                !acc.getVerificationCode().equals(req.getOtp())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .code(2002)
                            .message("Mã xác thực không hợp lệ")
                            .build()
            );
        }


        if (acc.getVerificationExpiry() == null ||
                acc.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .code(2002)
                            .message("Mã xác minh đã hết hạn!")
                            .build()
            );
        }


        acc.setEmail(req.getEmail());
        acc.setVerificationCode(null);
        acc.setVerificationExpiry(null);
        accountRepository.save(acc);


        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Email đã được xác minh và cập nhật thành công!")
                        .result("SUCCESS")
                        .build()
        );
    }
    /* ================= 6. UPLOAD AVATAR======================================== */
    @PostMapping("/avatar")
    @Transactional
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    ApiResponse.<String>builder()
                            .code(401)
                            .message("Unauthorized: No user in context")
                            .build()
            );
        }


        var account = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        try {
            String uploadDir = "uploads/";
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
            }

            String fileName = "avatar_" + account.getAccountID() + "_" + System.currentTimeMillis()
                    + "_" + file.getOriginalFilename();


            java.nio.file.Path path = dir.resolve(fileName);
            java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + fileName;
            account.setAvatarUrl(fileUrl);
            accountRepository.save(account);


            return ResponseEntity.ok(
                    ApiResponse.<String>builder()
                            .code(1000)
                            .message("Avatar uploaded successfully")
                            .result(fileUrl)
                            .build()
            );
        } catch (Exception e) {
            log.error("Upload avatar error", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.<String>builder()
                            .code(9999)
                            .message("Upload failed: " + e.getMessage())
                            .build()
            );
        }
    }


}

