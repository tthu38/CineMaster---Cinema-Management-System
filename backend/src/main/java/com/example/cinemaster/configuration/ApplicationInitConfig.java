package com.example.cinemaster.configuration;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner initAdmin(AccountRepository accountRepository, RoleRepository roleRepository) {
        return args -> {
            Optional<Account> existingAdmin = accountRepository.findByEmail("admin@cinemaster.com");

            if (existingAdmin.isEmpty()) {
                Role adminRole = roleRepository.findByRoleName("Admin")
                        .orElseGet(() -> {
                            Role newRole = Role.builder()
                                    .roleName("Admin")
                                    .build();
                            return roleRepository.save(newRole);
                        });

                Account admin = Account.builder()
                        .email("admin@cinemaster.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Administrator")
                        .phoneNumber("0900000000")
                        .isActive(true)
                        .createdAt(LocalDate.now())
                        .role(adminRole)
                        .loyaltyPoints(0)
                        .build();

                accountRepository.save(admin);

                log.warn("Admin account has been created: email=admin@cinemaster.com, password=admin123");
                log.warn("Please change this password immediately for security reasons!");
            } else {
                log.info("Admin account already exists, skipping creation.");
            }
        };
    }
}
