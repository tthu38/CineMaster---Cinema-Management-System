
package com.example.cinemaster.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DayScheduleResponse(
        LocalDate date,
        List<MovieSlots> movies
){
    public record MovieSlots(
            Integer movieId,
            String movieTitle,
            String posterUrl,
            List<SlotItem> slots
    ){}

    public record SlotItem(
            Integer showtimeId,
            Integer auditoriumId,
            String auditoriumName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer availableSeats,
            Integer totalSeats
    ){}
}
