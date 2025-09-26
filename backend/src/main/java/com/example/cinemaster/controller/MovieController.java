package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        ApiResponse<List<Movie>> res = new ApiResponse<>();
        res.setCode(1000);
        res.setMessage("Success");
        res.setResult(movieRepository.findAll());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/now-showing")
    public ResponseEntity<ApiResponse<List<Movie>>> getNowShowing() {
        ApiResponse<List<Movie>> res = new ApiResponse<>();
        res.setCode(1000);
        res.setMessage("Success");
        res.setResult(movieRepository.findByStatusIgnoreCase("Now Showing"));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<ApiResponse<List<Movie>>> getComingSoon() {
        ApiResponse<List<Movie>> res = new ApiResponse<>();
        res.setCode(1000);
        res.setMessage("Success");
        res.setResult(movieRepository.findByStatusIgnoreCase("Coming Soon"));
        return ResponseEntity.ok(res);
    }
}
