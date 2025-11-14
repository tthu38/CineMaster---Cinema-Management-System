package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ChangePasswordRequest;
import com.example.cinemaster.dto.request.EmailRequest;
import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.mapper.AccountMapper;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.MembershipRepository;
import com.example.cinemaster.security.AccountPrincipal;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MembershipRepository membershipRepository;

    /* =====================================================
                     HELPER – Lấy Principal
    ===================================================== */
    private Account getCurrentAccount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        Object principal = auth.getPrincipal();
        if (!(principal instanceof AccountPrincipal accPrincipal)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return accountRepository.findById(accPrincipal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public ProfileResponse getProfile() {
        Account acc = getCurrentAccount();

        // ===== LẤY MEMBERSHIP =====
        var membershipOpt = membershipRepository.findByAccount_AccountID(acc.getAccountID());

        int points = acc.getLoyaltyPoints() != null ? acc.getLoyaltyPoints() : 0; // fallback

        if (membershipOpt.isPresent()) {
            points = membershipOpt.get().getPoints();  // LẤY POINTS TỪ MEMBERSHIP
            acc.setLoyaltyPoints(points);             // ĐỒNG BỘ VỀ Accounts
            accountRepository.save(acc);
        }

        // ========= BUILD RESPONSE FORMAT CŨ =========
        return ProfileResponse.builder()
                .id(acc.getAccountID())
                .email(acc.getEmail())
                .fullName(acc.getFullName())
                .phoneNumber(acc.getPhoneNumber())
                .address(acc.getAddress())
                .roleName(acc.getRole().getRoleName())
                .createdAt(acc.getCreatedAt())
                .loyaltyPoints(points)       // FE ĐANG DÙNG FIELD NÀY
                .avatarUrl(acc.getAvatarUrl())
                .build();
    }


    /* =====================================================
                     UPDATE PROFILE
    ===================================================== */
    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest req, MultipartFile avatar) {

        Account acc = getCurrentAccount();

        // MapStruct cập nhật từ req sang acc
        accountMapper.updateAccount(acc, req);

        // Upload avatar nếu có
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String filename = "avatar_" + acc.getAccountID() + "_" +
                        System.currentTimeMillis() + "_" + avatar.getOriginalFilename();

                Path uploadDir = Paths.get("uploads");
                Files.createDirectories(uploadDir);

                Path filePath = uploadDir.resolve(filename);
                Files.copy(avatar.getInputStream(), filePath);

                acc.setAvatarUrl("/uploads/" + filename);
            } catch (Exception e) {
                throw new RuntimeException("Upload avatar failed", e);
            }
        }

        accountRepository.save(acc);
        return accountMapper.toProfileResponse(acc);
    }

    /* =====================================================
                     CHANGE PASSWORD
    ===================================================== */
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        Account acc = getCurrentAccount();

        if (!passwordEncoder.matches(req.getCurrentPassword(), acc.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        acc.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(acc);
    }

    /* =====================================================
                     SEND OTP CHANGE EMAIL
    ===================================================== */
    @Transactional
    public void sendOtpToChangeEmail(EmailRequest req) {

        Account acc = getCurrentAccount();

        if (accountRepository.existsByEmail(req.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        acc.setVerificationCode(otp);
        acc.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));
        accountRepository.save(acc);

        try {
            emailService.sendVerificationEmail(req.getEmail(), otp);
        } catch (MessagingException e) {
            log.error("Email send failed: {}", e.getMessage());
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /* =====================================================
                     VERIFY EMAIL CHANGE
    ===================================================== */
    @Transactional
    public void verifyEmailChange(EmailRequest req) {

        Account acc = getCurrentAccount();

        if (acc.getVerificationCode() == null ||
                !acc.getVerificationCode().equals(req.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        if (acc.getVerificationExpiry() == null ||
                acc.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        acc.setEmail(req.getEmail());
        acc.setVerificationCode(null);
        acc.setVerificationExpiry(null);

        accountRepository.save(acc);
    }

    /* =====================================================
                     UPLOAD AVATAR
    ===================================================== */
    @Transactional
    public String uploadAvatar(MultipartFile file) {

        Account acc = getCurrentAccount();

        try {
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);

            String fileName =
                    "avatar_" + acc.getAccountID() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            Path filePath = dir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String url = "/uploads/" + fileName;
            acc.setAvatarUrl(url);
            accountRepository.save(acc);

            return url;

        } catch (Exception e) {
            throw new RuntimeException("Upload avatar failed", e);
        }
    }
}
