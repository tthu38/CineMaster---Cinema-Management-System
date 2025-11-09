package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieRecommendResponse {
    String title;
    String genre;
    Double rating;
    String description;
    String posterUrl;
}
