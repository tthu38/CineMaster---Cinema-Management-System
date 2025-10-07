package com.example.cinemaster.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsDetailResponse {
    private Integer id;
    private String sectionTitle;
    private String sectionContent;
    private String imageUrl;
    private Integer displayOrder;
}
