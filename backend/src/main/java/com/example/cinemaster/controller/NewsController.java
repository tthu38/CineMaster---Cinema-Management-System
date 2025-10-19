package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.NewsRequest; // 👈 thêm import này
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.NewsResponse;
import com.example.cinemaster.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public ApiResponse<List<NewsResponse>> getAll(@RequestParam(required = false) String category) {
        return new ApiResponse<>(1000, "Success", newsService.getAll(category));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsResponse> getById(@PathVariable Integer id) {
        return new ApiResponse<>(1000, "Success", newsService.getById(id));
    }

    // 🔹 Thêm mới — tăng lượt xem
    @PutMapping("/{id}/view")
    public ApiResponse<Void> increaseView(@PathVariable Integer id) {
        newsService.increaseView(id);
        return new ApiResponse<>(1000, "View increased", null);
    }

    // Tạo mới tin tức
    @PreAuthorize("hasRole('Admin')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ApiResponse<NewsResponse> create(
            @RequestPart("data") NewsRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return new ApiResponse<>(1000, "Success", newsService.create(request, imageFile));
    }

    // Cập nhật tin tức
    @PreAuthorize("hasRole('Admin')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ApiResponse<NewsResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") NewsRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return new ApiResponse<>(1000, "Success", newsService.update(id, request, imageFile));
    }

    // Xoá tin tức
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        newsService.delete(id);
        return new ApiResponse<>(1000, "Success", null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Integer id) {
        newsService.restore(id);
        return new ApiResponse<>(1000, "Khôi phục thành công", null);
    }
}
