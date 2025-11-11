package com.example.cinemaster.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieFilterRequest {
    private String title;
    private String genre;
    private String director;
    private String cast;
    private String language;
}
