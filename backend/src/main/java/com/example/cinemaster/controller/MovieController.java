package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.MovieFilterRequest;
import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.repository.MovieRepository;
import com.example.cinemaster.service.FileStorageService;
import com.example.cinemaster.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        ApiResponse<List<Movie>> res = new ApiResponse<>(1000, "Success", movieRepository.findAll());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/now-showing")
    public ResponseEntity<ApiResponse<List<Movie>>> getNowShowing() {
        ApiResponse<List<Movie>> res = new ApiResponse<>(1000, "Success",
                movieRepository.findByStatusIgnoreCase("Now Showing"));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<ApiResponse<List<Movie>>> getComingSoon() {
        ApiResponse<List<Movie>> res = new ApiResponse<>(1000, "Success",
                movieRepository.findByStatusIgnoreCase("Coming Soon"));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Integer id) {
        try {
            MovieResponse movie = movieService.getById(id);
            return ResponseEntity.ok(new ApiResponse<>(1000, "Success", movie));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile
    ) {
        if (posterFile != null && !posterFile.isEmpty()) {
            String posterUrl = fileStorageService.savePosterFile(posterFile);
            request.setPosterUrl(posterUrl);
        }
        MovieResponse created = movieService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(1000, "Created", created));
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Integer id,
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile
    ) {
        if (posterFile != null && !posterFile.isEmpty()) {
            String posterUrl = fileStorageService.savePosterFile(posterFile);
            request.setPosterUrl(posterUrl);
        }
        MovieResponse updated = movieService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(1000, "Updated", updated));
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Integer id) {
        movieService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(1000, "Deleted", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) String cast,
            @RequestParam(required = false) String language) {

        MovieFilterRequest request = MovieFilterRequest.builder()
                .title(title)
                .genre(genre).director(director).cast(cast).language(language).build();

        List<MovieResponse> filteredMovies = movieService.filterMovies(request);

        ApiResponse<List<MovieResponse>> res = new ApiResponse<>();
        res.setCode(1000);
        res.setMessage("Success");
        res.setResult(filteredMovies);
        return ResponseEntity.ok(res);
    }
}
