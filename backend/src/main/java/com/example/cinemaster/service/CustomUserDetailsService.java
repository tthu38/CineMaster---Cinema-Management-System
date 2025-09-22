package com.example.cinemaster.service;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Sử dụng phương thức findByEmailWithRole để tránh lỗi LazyInitializationException
        Optional<Account> accountOpt = accountRepository.findByEmailWithRole(email);

        if (accountOpt.isEmpty()) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
        }

        Account account = accountOpt.get();

        Set<GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().getRoleName().toUpperCase())
        );

        return new User(account.getEmail(), account.getPassword(), authorities);
    }
}