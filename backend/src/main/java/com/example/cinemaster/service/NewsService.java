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


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsDetailRepository newsDetailRepository;
    private final NewsMapper newsMapper;
    private final FileStorageService fileStorageService;


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

    // =======================================================
    // 🟢 CREATE NEWS — Upload ảnh Cloudinary
    // =======================================================
    @Transactional
    public NewsResponse create(NewsRequest req, MultipartFile imageFile) {

        News news = new News();
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setRemark(req.getRemark());
        news.setViews(0);

        LocalDateTime now = LocalDateTime.now();
        news.setPublishDate(req.getPublishDate() != null ? req.getPublishDate() : now);
        news.setActive(true);

        // ⭐ UPLOAD ẢNH NEWS LÊN CLOUDINARY
        if (imageFile != null && !imageFile.isEmpty()) {
            String url = fileStorageService.saveNewsCloudinary(imageFile);
            news.setImageUrl(url);
        }

        newsRepository.save(news);

        // 📝 Lưu chi tiết section
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

    // =======================================================
    // 🟡 UPDATE NEWS — XÓA ẢNH CŨ + UPLOAD ẢNH MỚI
    // =======================================================
    @Transactional
    public NewsResponse update(Integer id, NewsRequest req, MultipartFile imageFile) {

        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setRemark(req.getRemark());
        news.setUpdatedDate(LocalDateTime.now());

        if (req.getPublishDate() != null) {
            news.setPublishDate(req.getPublishDate());
        }

        String oldImage = news.getImageUrl();

        // ⭐ NẾU CÓ ẢNH MỚI → XÓA ẢNH CŨ + UPLOAD ẢNH MỚI
        if (imageFile != null && !imageFile.isEmpty()) {

            // 1) XÓA ảnh cũ Cloudinary
            fileStorageService.deleteNewsCloudinary(oldImage);

            // 2) Upload ảnh mới
            String newUrl = fileStorageService.saveNewsCloudinary(imageFile);
            news.setImageUrl(newUrl);

        } else {
            // Giữ ảnh cũ
            news.setImageUrl(oldImage);
        }

        // Refresh details
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
        news.setActive(false);
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

        if (news.getViews() == null) news.setViews(0);

        news.setViews(news.getViews() + 1);
        news.setUpdatedDate(LocalDateTime.now());
        newsRepository.save(news);
    }
}

