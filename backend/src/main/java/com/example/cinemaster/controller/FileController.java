package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final AccountRepository accountRepository;

    @PutMapping("/avatar")
    @Transactional
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("avatarFile") MultipartFile avatarFile,
            @AuthenticationPrincipal Account account
    ) {
        ApiResponse<String> response = new ApiResponse<>();

        try {
            String avatarUrl = fileStorageService.saveFile(avatarFile);

            // cập nhật vào DB
            account.setAvatarUrl(avatarUrl);
            accountRepository.save(account);

            response.setCode(1000);
            response.setMessage("Upload avatar thành công");
            response.setResult(avatarUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.setCode(9999);
            response.setMessage("Upload avatar thất bại: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
