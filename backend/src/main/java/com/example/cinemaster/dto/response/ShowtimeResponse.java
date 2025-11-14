// src/main/java/com/example/cinemaster/dto/response/ShowtimeResponse.java
package com.example.cinemaster.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeResponse(
        Integer showtimeId,
        Integer periodId,
        Integer auditoriumId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String language,
        BigDecimal price,

        Integer movieId,
        String movieTitle,
        String posterUrl,

        String auditoriumName,
        Integer branchId,
        String branchName
) {}
