package com.example.cinemaster.dto.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsDetailRequest {
    private String sectionTitle;
    private String sectionContent;
    private String imageUrl;
    private Integer displayOrder;
}
