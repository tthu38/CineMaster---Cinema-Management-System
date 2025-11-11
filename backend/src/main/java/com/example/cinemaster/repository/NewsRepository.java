package com.example.cinemaster.repository;

import com.example.cinemaster.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Integer> {
    List<News> findByCategoryAndActiveTrue(String category);
    List<News> findByActiveTrue();
    List<News> findByCategory(String category);
}
