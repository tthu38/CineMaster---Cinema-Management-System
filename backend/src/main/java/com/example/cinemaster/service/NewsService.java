package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.NewsDetailRequest;
import com.example.cinemaster.dto.request.NewsRequest;
import com.example.cinemaster.dto.response.NewsResponse;
import com.example.cinemaster.entity.News;
import com.example.cinemaster.entity.NewsDetail;
import com.example.cinemaster.mapper.NewsMapper;
import com.example.cinemaster.repository.NewsRepository;
import com.example.cinemaster.repository.NewsDetailRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant; // 👈 bạn bị thiếu import này
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;
    private final NewsDetailRepository newsDetailRepository;
    private final NewsMapper newsMapper;
    private final FileStorageService fileStorageService; // 👈 thêm vào

    public List<NewsResponse> getAll(String category) {
        if (category != null && !category.isEmpty()) {
            return newsRepository.findByCategory(category).stream()
                    .map(newsMapper::toResponse)
                    .collect(Collectors.toList());
        }
        return newsRepository.findAll().stream()
                .map(newsMapper::toResponse)
                .collect(Collectors.toList());
    }


    public NewsResponse getById(Integer id) {
        var news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        NewsResponse response = newsMapper.toResponse(news);

        var details = newsDetailRepository.findByNewsID_NewsID(id).stream()
                .map(newsMapper::toDetailResponse)
                .collect(Collectors.toList());

        response.setDetails(details);
        return response;
    }

    @Transactional
    public NewsResponse create(NewsRequest req, MultipartFile imageFile) {
        News news = new News();
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setRemark(req.getRemark());
        news.setViews(0);

        LocalDateTime now = LocalDateTime.now();
//        news.setCreatedDate(now);
//        news.setUpdatedDate(now);
        // publishDate do user nhập, nếu null thì gán createdDate
        news.setPublishDate(req.getPublishDate() != null ? req.getPublishDate() : now);

        news.setActive(true);

        if (imageFile != null && !imageFile.isEmpty()) {
            String url = fileStorageService.saveNewsFile(imageFile);
            news.setImageUrl(url);
        }

        newsRepository.save(news);

        if (req.getDetails() != null) {
            for (NewsDetailRequest d : req.getDetails()) {
                NewsDetail detail = new NewsDetail();
                detail.setNewsID(news);
                detail.setSectionTitle(d.getSectionTitle());
                detail.setSectionContent(d.getSectionContent());
                detail.setImageUrl(d.getImageUrl());
                detail.setDisplayOrder(d.getDisplayOrder());
                newsDetailRepository.save(detail);
            }
        }

        return newsMapper.toResponse(news);
    }

    @Transactional
    public NewsResponse update(Integer id, NewsRequest req, MultipartFile imageFile) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setRemark(req.getRemark());

        LocalDateTime now = LocalDateTime.now();
        news.setUpdatedDate(now);
        // cho phép chỉnh sửa publishDate, nếu null thì giữ nguyên
        if (req.getPublishDate() != null) {
            news.setPublishDate(req.getPublishDate());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String url = fileStorageService.saveNewsFile(imageFile);
            news.setImageUrl(url);
        }

        // xoá detail cũ và thêm detail mới
        newsDetailRepository.deleteAll(newsDetailRepository.findByNewsID_NewsID(id));
        if (req.getDetails() != null) {
            for (NewsDetailRequest d : req.getDetails()) {
                NewsDetail detail = new NewsDetail();
                detail.setNewsID(news);
                detail.setSectionTitle(d.getSectionTitle());
                detail.setSectionContent(d.getSectionContent());
                detail.setImageUrl(d.getImageUrl());
                detail.setDisplayOrder(d.getDisplayOrder());
                newsDetailRepository.save(detail);
            }
        }

        return newsMapper.toResponse(news);
    }

    @Transactional
    public void delete(Integer id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
        news.setActive(false); // ❌ không xóa cứng
        news.setUpdatedDate(LocalDateTime.now());
        newsRepository.save(news);
    }

    @Transactional
    public void restore(Integer id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
        news.setActive(true);
        news.setUpdatedDate(LocalDateTime.now());
        newsRepository.save(news);
    }

    @Transactional
    public void increaseView(Integer id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        // Nếu null thì khởi tạo 0
        if (news.getViews() == null) news.setViews(0);

        news.setViews(news.getViews() + 1);
        news.setUpdatedDate(LocalDateTime.now());
        newsRepository.save(news);
    }

}