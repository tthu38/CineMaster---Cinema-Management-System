package com.example.cinemaster.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final String UPLOAD_DIR = "uploads";
    private final String POSTER_DIR = "posters";
    private final String NEWS_DIR = "news";

    private final Cloudinary cloudinary;  // 👉 THÊM CLOUDINARY

    public String saveFile(MultipartFile file) {
        return saveToDir(file, UPLOAD_DIR, "avatar_");
    }

    public String saveNewsFile(MultipartFile file) {
        return saveToDir(file, NEWS_DIR, "news_");
    }

    /**
     *  🚀 POSTER — Upload lên Cloudinary
     */
    public String savePosterFile(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", "posters"
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("Không thể upload poster lên Cloudinary!", e);
        }
    }

    /**
     *  🗑️ XÓA POSTER CŨ TRÊN CLOUDINARY
     */
    public void deletePosterCloudinary(String url) {
        if (url == null || !url.contains("cloudinary")) return;

        try {
            String publicId = extractPublicId(url);

            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image"));

        } catch (Exception e) {
            System.out.println("⚠ Không thể xóa poster cũ trên Cloudinary: " + e.getMessage());
        }
    }

    /**
     *  🗑️ XÓA TRAILER CŨ TRÊN CLOUDINARY (THÊM MỚI)
     */
    public void deleteTrailerCloudinary(String url) {
        if (url == null || !url.contains("cloudinary")) return;

        try {
            String publicId = extractPublicId(url);

            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "video"));

        } catch (Exception e) {
            System.out.println("⚠ Không thể xóa trailer cũ trên Cloudinary: " + e.getMessage());
        }
    }

    /**
     *  👉 Tách public_id từ URL Cloudinary
     */
    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/");
            String fileName = parts[parts.length - 1]; // abcxyz.jpg
            String folder = parts[parts.length - 2];   // posters hoặc trailers

            return folder + "/" + fileName.substring(0, fileName.lastIndexOf('.'));
        } catch (Exception e) {
            throw new RuntimeException("Không thể tách public_id từ URL: " + url);
        }
    }

    private String saveToDir(MultipartFile file, String baseDir, String prefix) {
        if (file.isEmpty()) throw new RuntimeException("Thư mục rỗng");

        try {
            File dir = new File(baseDir);
            if (!dir.exists()) dir.mkdirs();

            String ext = getFileExtension(file.getOriginalFilename());
            String filename = prefix + UUID.randomUUID() + ext;

            Path path = Paths.get(baseDir, filename);
            Files.copy(file.getInputStream(), path);

            return "/" + baseDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file. Error: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex >= 0) ? filename.substring(dotIndex) : "";
    }

    //news
    /**
     *  🚀 NEWS IMAGE — Upload lên Cloudinary
     */
    public String saveNewsCloudinary(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", "news"
                    )
            );
            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("Không thể upload ảnh NEWS lên Cloudinary!", e);
        }
    }

    /**
     *  🗑️ XÓA ẢNH NEWS CŨ TRÊN CLOUDINARY
     */
    public void deleteNewsCloudinary(String url) {
        if (url == null || !url.contains("cloudinary")) return;

        try {
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            System.out.println("⚠ Không thể xóa ảnh NEWS trên Cloudinary: " + e.getMessage());
        }
    }

}

