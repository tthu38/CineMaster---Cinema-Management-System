package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieResponse {
    Integer movieId;
    String title;
    String genre;
    Integer duration;
    LocalDate releaseDate;
    String director;
    String cast;
    String description;
    String language;
    String ageRestriction;
    String country;
    String trailerUrl;
    String posterUrl;
    String status;
}
