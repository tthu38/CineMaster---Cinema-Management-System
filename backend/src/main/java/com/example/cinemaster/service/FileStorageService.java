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

    private final String UPLOAD_DIR = "uploads"; // thư mục ngay trong backend project

    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        // Tạo thư mục nếu chưa có
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Đặt tên file unique
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "avatar_" + UUID.randomUUID() + ext;

        Path path = Paths.get(UPLOAD_DIR, filename);
        Files.copy(file.getInputStream(), path);

        // Trả về URL để FE dùng hiển thị
        return "/uploads/" + filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex >= 0) ? filename.substring(dotIndex) : "";
    }
}
