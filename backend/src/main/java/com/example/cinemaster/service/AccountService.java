package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.AccountDTO;
import com.example.cinemaster.dto.request.AccountUpdateDTO;
import com.example.cinemaster.dto.request.PasswordUpdateDTO;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${app.upload.dir:./images}")
    private String uploadDir;

    public AccountDTO getAccountProfile(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with email: " + email));
        return modelMapper.map(account, AccountDTO.class);
    }

    public AccountDTO updateAccountProfile(String email, AccountUpdateDTO updateDTO) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with email: " + email));

        // Check if the new email already exists and is not the current user's email
        if (!account.getEmail().equals(updateDTO.getEmail()) && accountRepository.existsByEmail(updateDTO.getEmail())) {
            throw new IllegalArgumentException("Email already taken.");
        }

        account.setFullName(updateDTO.getFullName());
        account.setPhoneNumber(updateDTO.getPhoneNumber());
        account.setEmail(updateDTO.getEmail());
        account.setAddress(updateDTO.getAddress());

        Account updatedAccount = accountRepository.save(account);
        return modelMapper.map(updatedAccount, AccountDTO.class);
    }

    public void updateAccountPassword(String email, PasswordUpdateDTO passwordUpdateDTO) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with email: " + email));

        if (!passwordEncoder.matches(passwordUpdateDTO.getCurrentPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        if (!passwordUpdateDTO.getNewPassword().equals(passwordUpdateDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }

        account.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        accountRepository.save(account);
    }

    public String saveAvatar(MultipartFile file, Integer accountId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with ID: " + accountId));

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath); // Create directory if it doesn't exist

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = "/images/" + fileName; // Path relative to static
        account.setAvatarUrl(relativePath);
        accountRepository.save(account);
        return relativePath;
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }
}