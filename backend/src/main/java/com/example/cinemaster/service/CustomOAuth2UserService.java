package com.example.cinemaster.service;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // Lấy thông tin user từ Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");
        if (googleId == null) {
            googleId = oAuth2User.getAttribute("id"); // dự phòng
        }

        // Tìm account theo email
        Account account = accountRepository.findByEmailWithRole(email).orElse(null);


        // Nếu chưa có account, tạo mới
        if (account == null) {
            Account newAccount = new Account();
            newAccount.setEmail(email);
            newAccount.setFullName(oAuth2User.getAttribute("name"));
            newAccount.setCreatedAt(LocalDate.now());
            newAccount.setIsActive(true);

            // Gán role mặc định "Customer"
            Role defaultRole = roleRepository.findByRoleName("Customer")
                    .orElseThrow(() -> new RuntimeException("Default role Customer not found"));
            newAccount.setRole(defaultRole);

            // Gán Google ID vào googleAuth
            newAccount.setGoogleAuth(googleId);

            account = accountRepository.save(newAccount);
        }

        // Nếu account đã tồn tại nhưng chưa có googleAuth, cập nhật luôn
        else if (account.getGoogleAuth() == null) {
            account.setGoogleAuth(googleId);
            accountRepository.save(account);
        }

        return oAuth2User;
    }
}