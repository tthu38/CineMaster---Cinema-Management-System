package com.example.cinemaster.dto.response;

import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsResponse {
    private Integer newsID;
    private String title;
    private String content;
    private String category;
    private String imageUrl;
    private LocalDateTime publishDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Integer views;
    private Boolean active;

    private List<NewsDetailResponse> details;
}