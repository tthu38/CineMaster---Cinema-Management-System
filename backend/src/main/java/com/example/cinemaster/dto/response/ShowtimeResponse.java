package com.example.cinemaster.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShowtimeResponse {
    private String movieTitle;
    private String branchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String languages;
    private Double price;

    public ShowtimeResponse(String movieTitle,
                            String branchName,
                            LocalDateTime startTime,
                            LocalDateTime endTime,
                            String languages,
                            BigDecimal price) {
        this.movieTitle = movieTitle;
        this.branchName = branchName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.languages = languages;
        this.price = (price != null ? price.doubleValue() : null);
    }

}
