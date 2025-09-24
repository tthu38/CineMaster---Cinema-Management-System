package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.LoginRequest;
import com.example.cinemaster.dto.response.AuthResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.util.LoginUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        String phone = LoginUtil.normalizePhoneVN(request.getPhoneNumber());
        if (phone == null) throw new BadCredentialsException("Số điện thoại không hợp lệ");

        Account acc = accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new BadCredentialsException("Sai số điện thoại hoặc tài khoản bị khoá"));

        if (!passwordEncoder.matches(request.getPassword(), acc.getPassword())) {
            throw new BadCredentialsException("Sai mật khẩu");
        }

        String role = acc.getRole() != null ? acc.getRole().getRoleName() : "Customer";
        String token = jwtService.generateAccessToken(acc.getAccountID(), acc.getPhoneNumber(), role);

        return new AuthResponse(token, "Bearer", 60 * 60);
    }

    public Account getCurrentUser() {
        String phone = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
}
