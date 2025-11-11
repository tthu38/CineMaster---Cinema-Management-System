package com.example.cinemaster.dto.request;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MovieFeedbackRequest {
    private Integer accountId;
    private Integer rating;
    private String comment;
}
