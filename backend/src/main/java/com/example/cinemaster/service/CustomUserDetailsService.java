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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Tìm bằng email
        Optional<Account> accountOpt = accountRepository.findByEmail(usernameOrEmail);

        // Nếu không tìm thấy, thử tìm bằng số điện thoại
        if (accountOpt.isEmpty()) {
            accountOpt = accountRepository.findByPhoneNumberWithRole(usernameOrEmail);
        }

        if (accountOpt.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Không tìm thấy tài khoản với email hoặc số điện thoại: " + usernameOrEmail);
        }

        Account account = accountOpt.get();

        GrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + account.getRole().getRoleName().toUpperCase()
        );

        return new User(account.getEmail() != null ? account.getEmail() : account.getPhoneNumber(),
                account.getPassword(),
                Collections.singleton(authority));
    }
}
