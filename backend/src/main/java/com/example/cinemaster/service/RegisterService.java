package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.RegisterRequest;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.RoleRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class RegisterService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // đăng ký account mới
    public String register(RegisterRequest request) {
        // chỉ check số điện thoại trùng
        if (accountRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return "Số điện thoại đã tồn tại!";
        }

        // lấy role mặc định Customer
        Optional<Role> roleOpt = roleRepository.findByRoleName("Customer");
        if (roleOpt.isEmpty()) {
            return "Không tìm thấy Role Customer trong hệ thống!";
        }

        // tạo account mới
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());
        account.setPhoneNumber(request.getPhoneNumber());
        account.setIsActive(false);
        account.setCreatedAt(LocalDate.now());
        account.setRole(roleOpt.get());

        // tạo mã xác thực 6 chữ số
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        account.setVerificationCode(code);
        account.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));

        // lưu vào DB
        accountRepository.save(account);

        // gửi email xác thực
        try {
            emailService.sendVerificationEmail(request.getEmail(), code);
        } catch (MessagingException e) {
            return "Đăng ký thành công nhưng gửi email thất bại: " + e.getMessage();
        }

        return "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.";
    }

    // verify account
    public String verifyAccount(String email, String code) {
        // lấy account mới nhất theo email và chưa kích hoạt
        Optional<Account> accountOpt = accountRepository.findLatestByEmail(email);
        if (accountOpt.isEmpty()) {
            return "Email không tồn tại hoặc tài khoản đã được kích hoạt!";
        }

        Account account = accountOpt.get();

        if (!code.equals(account.getVerificationCode())) {
            return "Mã xác thực không đúng!";
        }

        if (account.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            return "Mã xác thực đã hết hạn!";
        }

        account.setIsActive(true);
        account.setVerificationCode(null);
        accountRepository.save(account);

        return "Xác thực thành công! Tài khoản đã được kích hoạt.";
    }
}