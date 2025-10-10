package com.example.cinemaster.dto.response;

import java.util.List;

public record MovieDaySchedule(
        Integer movieId,
        String movieTitle,
        String posterUrl,
        List<ShowtimeSlot> slots
){}
