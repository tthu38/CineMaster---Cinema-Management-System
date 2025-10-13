// src/main/java/com/example/cinemaster/mapper/ShowtimeMapper.java
package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Showtime;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeMapper {
    public ShowtimeResponse toResponse(Showtime s) {
        String movieTitle = null;
        String auditoriumName = null;
        Integer branchId = null;

        if (s.getPeriod() != null && s.getPeriod().getMovie() != null) {
            movieTitle = s.getPeriod().getMovie().getTitle();
        }
        if (s.getAuditorium() != null) {
            auditoriumName = s.getAuditorium().getName();
            if (s.getAuditorium().getBranch() != null) {
                branchId = s.getAuditorium().getBranch().getId();
            }
        }

        return new ShowtimeResponse(
                s.getShowtimeID(),
                s.getPeriod() != null ? s.getPeriod().getId() : null,
                s.getAuditorium() != null ? s.getAuditorium().getAuditoriumID() : null,
                s.getStartTime(),
                s.getEndTime(),
                s.getLanguage(),
                s.getPrice(),
                movieTitle,
                auditoriumName,
                branchId
        );
    }
}
