package com.example.cinemaster.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trailers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrailerController {

    private final Cloudinary cloudinary;

    /**
     * Upload trailer video lên Cloudinary và trả về URL.
     * Giữ nguyên giao diện, chỉ đổi nguồn lưu trữ video.
     */
    @PostMapping("/upload")
    public String uploadTrailer(@RequestParam("file") MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "trailers"
                ));
        return uploadResult.get("secure_url").toString();
    }
}
