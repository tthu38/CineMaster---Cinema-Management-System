package com.example.cinemaster.service;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.util.LoginUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneRaw) throws UsernameNotFoundException {
        String phone = LoginUtil.normalizePhoneVN(phoneRaw);

        Account acc = accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy SĐT hoặc bị khoá"));

        String roleName = acc.getRole() != null ? acc.getRole().getRoleName() : "USER";
        return new User(
                acc.getPhoneNumber(),
                acc.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }


}
