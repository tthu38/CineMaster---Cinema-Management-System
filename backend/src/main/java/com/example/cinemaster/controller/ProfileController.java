package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ChangePasswordRequest;
import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.DiscountService;
import com.example.cinemaster.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final DiscountService discountService;


    /* =================== 1. GET PROFILE =================== */
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

        ProfileResponse profile = profileService.getProfile();

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .code(200)
                        .message("Thành công!")
                        .result(profile)
                        .build()
        );
    }

    /* =================== 2. UPDATE PROFILE =================== */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody @Valid UpdateProfileRequest req,
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

        ProfileResponse updated = profileService.updateProfile(req, null);

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .code(1000)
                        .message("Cập nhật thành công!")
                        .result(updated)
                        .build()
        );
    }

    /* =================== 3. CHANGE PASSWORD =================== */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest req,
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

        profileService.changePassword(req);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Thay đổi mật khẩu thành công")
                        .result("OK")
                        .build()
        );
    }

    /* =================== 4. SEND OTP CHANGE EMAIL =================== */
    @PostMapping("/profile/send-otp-change-email")
    public ResponseEntity<ApiResponse<String>> sendOtpChangeEmail(
            @RequestBody @Valid EmailRequest req,
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

        profileService.sendOtpToChangeEmail(req);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Đã gửi OTP thành công")
                        .result("OK")
                        .build()
        );
    }

    /* =================== 5. VERIFY EMAIL CHANGE =================== */
    @PostMapping("/profile/verify-email-change")
    public ResponseEntity<ApiResponse<String>> verifyEmailChange(
            @RequestBody @Valid EmailRequest req,
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

        profileService.verifyEmailChange(req);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Email đã được xác minh và cập nhật thành công!")
                        .result("SUCCESS")
                        .build()
        );
    }

    /* =================== 6. UPLOAD AVATAR =================== */
    @PostMapping("/avatar")
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

        String url = profileService.uploadAvatar(file);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(1000)
                        .message("Avatar uploaded successfully")
                        .result(url)
                        .build()
        );
    }
    /* =================== GET PROMOTIONS (for profile page) =================== */
    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getPromotions() {
        List<DiscountResponse> promos = discountService.getAll();

        return ResponseEntity.ok(
                ApiResponse.<List<DiscountResponse>>builder()
                        .code(200)
                        .message("Success")
                        .result(promos)
                        .build()
        );
    }

}
