package com.example.cinemaster.dto.request;

import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsRequest {
    private String title;
    private String content;
    private String category;
    private String remark;
    private Boolean active;
    private LocalDateTime publishDate;
    private List<NewsDetailRequest> details;
}
