package com.example.cinemaster.repository;

import com.example.cinemaster.entity.NewsDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsDetailRepository extends JpaRepository<NewsDetail, Integer> {
    List<NewsDetail> findByNewsID_NewsID(Integer newsId);
}
