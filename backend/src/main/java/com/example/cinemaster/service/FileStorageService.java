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

    private final String UPLOAD_DIR = "uploads";  // Avatar
    private final String POSTER_DIR = "posters";  // Poster phim
    private final String NEWS_DIR = "news";        // Ảnh tin tức

    // Lưu avatar
    public String saveFile(MultipartFile file) {
        return saveToDir(file, UPLOAD_DIR, "avatar_");
    }

    // Lưu poster
    public String savePosterFile(MultipartFile file) {
        return saveToDir(file, POSTER_DIR, "poster_");
    }

    // Lưu news
    public String saveNewsFile(MultipartFile file) {
        return saveToDir(file, NEWS_DIR, "news_");
    }

    private String saveToDir(MultipartFile file, String baseDir, String prefix) {
        if (file.isEmpty()) throw new RuntimeException("File is empty");

        try {
            File dir = new File(baseDir);
            if (!dir.exists()) dir.mkdirs();

            String ext = getFileExtension(file.getOriginalFilename());
            String filename = prefix + UUID.randomUUID() + ext;

            Path path = Paths.get(baseDir, filename);
            Files.copy(file.getInputStream(), path);

            return "/" + baseDir + "/" + filename; // path trả về cho FE
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