package com.example.cinemaster.repository;

import com.example.cinemaster.entity.MovieFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieFeedbackRepository extends JpaRepository<MovieFeedback, Integer> {
    List<MovieFeedback> findByMovie_MovieID(Integer movieId);
}
