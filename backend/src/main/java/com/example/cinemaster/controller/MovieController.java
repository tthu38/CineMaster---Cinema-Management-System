package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.service.FileStorageService;
import com.example.cinemaster.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>(1000, "Success", movieService.getAll(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(1000, "Success", movieService.getById(id)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile) {
        if (posterFile != null && !posterFile.isEmpty()) {
            String posterUrl = fileStorageService.savePosterFile(posterFile);
            request.setPosterUrl(posterUrl);
        }
        return ResponseEntity.ok(new ApiResponse<>(1000, "Created", movieService.create(request)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Integer id,
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile) {
        if (posterFile != null && !posterFile.isEmpty()) {
            String posterUrl = fileStorageService.savePosterFile(posterFile);
            request.setPosterUrl(posterUrl);
        }
        return ResponseEntity.ok(new ApiResponse<>(1000, "Updated", movieService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Integer id) {
        movieService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(1000, "Deleted", null));
    }
}
