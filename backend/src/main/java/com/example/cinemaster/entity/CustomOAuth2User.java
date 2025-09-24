package com.example.cinemaster.entity;

import com.example.cinemaster.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User; // thông tin gốc từ Google
    private final Account account;        // account trong DB

    public CustomOAuth2User(OAuth2User oauth2User, Account account) {
        this.oauth2User = oauth2User;
        this.account = account;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return account.getFullName(); // lấy từ DB
    }

    // Lấy email từ DB
    public String getEmail() {
        return account.getEmail();
    }

    // Lấy luôn Account nếu cần
    public Account getAccount() {
        return account;
    }
}
