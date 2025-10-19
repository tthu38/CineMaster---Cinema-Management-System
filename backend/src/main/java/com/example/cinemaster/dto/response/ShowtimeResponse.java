// src/main/java/com/example/cinemaster/dto/response/ShowtimeResponse.java
package com.example.cinemaster.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeResponse(
        // ===== Thông tin suất chiếu =====
        Integer showtimeId,
        Integer periodId,
        Integer auditoriumId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String language,
        BigDecimal price,

        // ===== Thông tin phim =====
        Integer movieId,
        String movieTitle,
        String posterUrl,

        // ===== Thông tin phòng & chi nhánh =====
        String auditoriumName,
        Integer branchId

) {}
