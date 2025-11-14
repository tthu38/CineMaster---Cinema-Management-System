package com.example.cinemaster.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieRecommendResponse {
    private Integer movieId;
    String title;
    String genre;
    Double rating;
    String description;
    String posterUrl;


    public MovieRecommendResponse(Integer movieId, String title, String genre, Double rating) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.rating = rating;
    }
}

