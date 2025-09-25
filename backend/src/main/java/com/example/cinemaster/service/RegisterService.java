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

    public String register(RegisterRequest request) {
        if (accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return "Số điện thoại đã tồn tại";
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            return "Email đã tồn tại";
        }


        Role role = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role"));

        Account account = buildNewAccount(request, role);

        accountRepository.save(account);

        try {
            emailService.sendVerificationEmail(
                    account.getEmail(),
                    account.getVerificationCode()
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email xác thực", e);
        }

        return "Đăng ký thành công, vui lòng kiểm tra email để xác thực!";
    }

    public String verifyAccount(String email, String code) {
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
        account.setVerificationExpiry(null);
        accountRepository.save(account);

        return "Xác thực thành công! Tài khoản đã được kích hoạt.";
    }

    private Account buildNewAccount(RegisterRequest request, Role role) {
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());
        account.setPhoneNumber(request.getPhoneNumber());
        account.setIsActive(false); // mới đăng ký -> false
        account.setCreatedAt(LocalDate.now());
        account.setRole(role);

        String code = generateVerificationCode();
        account.setVerificationCode(code);
        account.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));

        return account;
    }

    private String generateVerificationCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
