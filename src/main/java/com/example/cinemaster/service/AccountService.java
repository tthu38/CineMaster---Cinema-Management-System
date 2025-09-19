package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.RegisterRequest;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailService emailService;

    public String register(RegisterRequest request) {
        // check email đã tồn tại chưa
        Optional<Account> existing = accountRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            return "Email đã được đăng ký!";
        }

        // tạo mã xác thực
        String verificationCode = generateVerificationCode();

        // tạo account
        Account acc = new Account();
        acc.setEmail(request.getEmail());
        acc.setPassword(PasswordEncoderUtil.encode(request.getPassword())); // mã hóa
        acc.setFullName(request.getFullName());
        acc.setPhoneNumber(request.getPhoneNumber());
        acc.setAddress(request.getAddress());
        acc.setCreateAt(LocalDate.now());
        acc.setIsActive(false); // mặc định chưa active
        acc.setVerificationCode(verificationCode);
        acc.setVerificationExpiry(Instant.now().plusSeconds(600)); // 10 phút

        accountRepository.save(acc);

        // gửi mail
        try {
            emailService.sendVerificationEmail(request.getEmail(), verificationCode);
        } catch (Exception e) {
            return "Đăng ký thất bại: lỗi gửi email";
        }

        return "Đăng ký thành công, vui lòng kiểm tra email để xác thực!";
    }

    public String verifyAccount(String email, String code) {
        Optional<Account> opt = accountRepository.findByEmail(email);
        if (opt.isEmpty()) return "Không tìm thấy tài khoản";

        Account acc = opt.get();

        if (acc.getVerificationCode() == null || !acc.getVerificationCode().equals(code)) {
            return "Mã xác thực không đúng!";
        }
        if (acc.getVerificationExpiry().isBefore(Instant.now())) {
            return "Mã xác thực đã hết hạn!";
        }

        acc.setIsActive(true);
        acc.setVerificationCode(null);
        acc.setVerificationExpiry(null);
        accountRepository.save(acc);

        return "Xác thực thành công, bạn có thể đăng nhập!";
    }

    private String generateVerificationCode() {
        int code = new Random().nextInt(999999);
        return String.format("%06d", code); // 6 chữ số
    }
}