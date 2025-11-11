package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.NewsDetailResponse;
import com.example.cinemaster.dto.response.NewsResponse;
import com.example.cinemaster.entity.News;
import com.example.cinemaster.entity.NewsDetail;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    public NewsResponse toResponse(News news) {
        return NewsResponse.builder()
                .newsID(news.getNewsID())
                .title(news.getTitle())
                .content(news.getContent())
                .category(news.getCategory())
                .imageUrl(news.getImageUrl())
                .publishDate(news.getPublishDate())
                .createdDate(news.getCreatedDate())
                .updatedDate(news.getUpdatedDate())
                .views(news.getViews())
                .active(news.getActive())
                .build();
    }

    public NewsDetailResponse toDetailResponse(NewsDetail d) {
        return NewsDetailResponse.builder()
                .id(d.getId())
                .sectionTitle(d.getSectionTitle())
                .sectionContent(d.getSectionContent())
                .imageUrl(d.getImageUrl())
                .displayOrder(d.getDisplayOrder())
                .build();
    }
}
