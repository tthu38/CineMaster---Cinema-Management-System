package com.example.cinemaster.repository;

import com.example.cinemaster.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Integer> {
    List<News> findByCategoryAndActiveTrue(String category);  // chỉ lấy active = true
    List<News> findByActiveTrue();                           // chỉ lấy active = true
    List<News> findByCategory(String category);              // 👈 lấy tất cả, bất kể active
}
