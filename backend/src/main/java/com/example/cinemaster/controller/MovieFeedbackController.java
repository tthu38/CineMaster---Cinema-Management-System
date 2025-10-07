package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.MovieFeedbackRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.MovieFeedbackResponse;
import com.example.cinemaster.service.JwtService;
import com.example.cinemaster.service.MovieFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class MovieFeedbackController {

    private final MovieFeedbackService feedbackService;
    private final JwtService jwtService; // 👈 Thêm vào để decode token

    // 🟢 Xem tất cả feedback của phim — public
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<MovieFeedbackResponse>>> getByMovie(@PathVariable Integer movieId) {
        return ResponseEntity.ok(
                new ApiResponse<>(1000, "Success", feedbackService.getByMovie(movieId))
        );
    }

    // 🟢 Tạo feedback — yêu cầu đăng nhập
    @PostMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<MovieFeedbackResponse>> create(
            @PathVariable Integer movieId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MovieFeedbackRequest request
    ) {
        // ✅ Kiểm tra login
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Bạn cần đăng nhập để gửi đánh giá!", null));
        }

        // 👇 Giải mã token để lấy accountId
        String token = authHeader.substring(7);
        Integer accountId = jwtService.extractAccountId(token);

        request.setAccountId(accountId);

        return ResponseEntity.ok(
                new ApiResponse<>(1000, "Created", feedbackService.create(movieId, request))
        );
    }

    // 🟢 Cập nhật feedback — yêu cầu đăng nhập
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieFeedbackResponse>> update(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MovieFeedbackRequest request
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Bạn cần đăng nhập để chỉnh sửa đánh giá!", null));
        }

        String token = authHeader.substring(7);
        Integer accountId = jwtService.extractAccountId(token);
        request.setAccountId(accountId);

        return ResponseEntity.ok(
                new ApiResponse<>(1000, "Updated", feedbackService.update(id, request))
        );
    }

    // 🟢 Xoá feedback — yêu cầu đăng nhập
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Bạn cần đăng nhập để xóa đánh giá!", null));
        }

        String token = authHeader.substring(7);
        Integer accountId = jwtService.extractAccountId(token);

        feedbackService.delete(id, accountId);
        return ResponseEntity.ok(new ApiResponse<>(1000, "Deleted", null));
    }
}
