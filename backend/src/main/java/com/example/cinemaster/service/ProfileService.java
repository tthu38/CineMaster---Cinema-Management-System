package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.mapper.AccountMapper;
import com.example.cinemaster.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    private String getCurrentPhone() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName(); // phoneNumber trong token
    }

    public ProfileResponse getProfile() {
        String phone = getCurrentPhone();
        Account acc = accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return accountMapper.toProfileResponse(acc);
    }

    public ProfileResponse updateProfile(UpdateProfileRequest req, MultipartFile avatarFile) {
        String phone = getCurrentPhone();
        Account acc = accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // MapStruct copy từ request sang entity
        accountMapper.updateAccount(acc, req);

        // Nếu có file avatar thì xử lý upload
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String filename = System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();
            Path filepath = Paths.get("uploads", filename);
            try {
                Files.createDirectories(filepath.getParent());
                Files.write(filepath, avatarFile.getBytes());
                acc.setAvatarUrl("/uploads/" + filename); // FE có thể load bằng URL này
            } catch (IOException e) {
                throw new RuntimeException("Upload avatar failed", e);
            }
        }

        accountRepository.save(acc);
        return accountMapper.toProfileResponse(acc);
    }

    public void deleteProfile() {
        String phone = getCurrentPhone();
        Account acc = accountRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        accountRepository.delete(acc);
    }
}
