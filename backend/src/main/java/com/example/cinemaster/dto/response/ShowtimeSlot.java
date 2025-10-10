package com.example.cinemaster.dto.response;

import java.time.LocalDateTime;

public record ShowtimeSlot(
        Integer showtimeId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer auditoriumId,
        String auditoriumName
){}
