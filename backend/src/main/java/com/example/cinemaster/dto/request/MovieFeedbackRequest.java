package com.example.cinemaster.dto.request;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MovieFeedbackRequest {
    private Integer accountId;  // tạm thời public, sau này lấy từ token
    private Integer rating;
    private String comment;
}
