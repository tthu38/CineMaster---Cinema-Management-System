package com.example.cinemaster.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String UPLOAD_DIR = "uploads"; // thư mục uploads trong project

    public String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            // Tạo thư mục nếu chưa tồn tại
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Tạo tên file unique
            String ext = getFileExtension(file.getOriginalFilename());
            String filename = "avatar_" + UUID.randomUUID() + ext;

            Path path = Paths.get(UPLOAD_DIR, filename);
            Files.copy(file.getInputStream(), path);

            // ✅ Trả về path chuẩn cho FE (bắt đầu bằng /uploads/)
            return "/uploads/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Could not store file. Error: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex >= 0) ? filename.substring(dotIndex) : "";
    }
}
