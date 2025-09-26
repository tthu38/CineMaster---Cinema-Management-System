package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
    List<Movie> findByStatusIgnoreCase(String status);
}
