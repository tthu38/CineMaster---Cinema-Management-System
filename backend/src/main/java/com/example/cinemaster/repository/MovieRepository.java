package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
    List<Movie> findByStatusIgnoreCase(String status);

    @Query("SELECT m FROM Movie m WHERE " +
            // 1. Thêm tham số Title vào đây
            "(:title IS NULL OR m.title LIKE %:title%) AND " +
            // 2. Các tham số còn lại
            "(:genre IS NULL OR m.genre LIKE %:genre%) AND " +
            "(:director IS NULL OR m.director LIKE %:director%) AND " +
            "(:cast IS NULL OR m.cast LIKE %:cast%) AND " +
            "(:language IS NULL OR m.language LIKE %:language%)")
    List<Movie> findMoviesByCriteria(
            // 1. Cập nhật tham số phương thức để bao gồm Title
            @Param("title") String title, // Thêm Title
            @Param("genre") String genre,
            @Param("director") String director,
            @Param("cast") String cast,
            @Param("language") String language
    );


}
