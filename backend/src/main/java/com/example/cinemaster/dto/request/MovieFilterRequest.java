package com.example.cinemaster.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieFilterRequest {
    private String title;
    private String genre;       // Thể loại (Genre)
    private String director;    // Đạo diễn (Director)
    private String cast;        // Diễn viên (Casts)
    private String language;    // Ngôn ngữ (Languages)
}
