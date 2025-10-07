package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieRequest {
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

