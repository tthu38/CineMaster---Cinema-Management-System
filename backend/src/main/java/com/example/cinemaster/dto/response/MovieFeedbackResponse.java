package com.example.cinemaster.dto.response;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MovieFeedbackResponse {
    private Integer id;
    private Integer accountId; // 👈 cần có để frontend kiểm tra quyền
    private String accountName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
