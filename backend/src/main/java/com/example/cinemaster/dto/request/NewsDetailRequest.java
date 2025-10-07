package com.example.cinemaster.dto.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsDetailRequest {
    private String sectionTitle;
    private String sectionContent;
    private String imageUrl;     // 👈 cho phép nhập URL ảnh section
    private Integer displayOrder;
}
