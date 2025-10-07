package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.mapper.MovieMapper;
import com.example.cinemaster.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    // poster mặc định khi không có ảnh
    private static final String DEFAULT_POSTER = "poster/default-poster.jpg";

    public MovieResponse create(MovieRequest request) {
        Movie movie = movieMapper.toEntity(request);

        // fallback poster nếu rỗng
        if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
            movie.setPosterUrl(DEFAULT_POSTER);
        }

        return movieMapper.toResponse(movieRepository.save(movie));
    }

    public MovieResponse update(Integer id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // giữ lại poster cũ
        String oldPosterUrl = movie.getPosterUrl();

        // cập nhật từ request
        movieMapper.updateEntity(movie, request);

        // nếu không truyền poster mới thì dùng poster cũ
        if (request.getPosterUrl() == null || request.getPosterUrl().isEmpty()) {
            movie.setPosterUrl(oldPosterUrl != null ? oldPosterUrl : DEFAULT_POSTER);
        }

        // fallback tránh null
        if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
            movie.setPosterUrl(DEFAULT_POSTER);
        }

        return movieMapper.toResponse(movieRepository.save(movie));
    }

    public void delete(Integer id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // chỉ đổi status, giữ nguyên poster
        movie.setStatus("Ended");

        // fallback nếu poster null
        if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
            movie.setPosterUrl(DEFAULT_POSTER);
        }

        movieRepository.save(movie);
    }

    public MovieResponse getById(Integer id) {
        return movieRepository.findById(id)
                .map(m -> {
                    if (m.getPosterUrl() == null || m.getPosterUrl().isEmpty()) {
                        m.setPosterUrl(DEFAULT_POSTER);
                    }
                    return movieMapper.toResponse(m);
                })
                .orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    public List<MovieResponse> getAll(String status) {
        List<Movie> movies = (status == null || status.isEmpty())
                ? movieRepository.findAll()
                : movieRepository.findByStatusIgnoreCase(status);

        // fallback cho tất cả record
        movies.forEach(m -> {
            if (m.getPosterUrl() == null || m.getPosterUrl().isEmpty()) {
                m.setPosterUrl(DEFAULT_POSTER);
            }
        });

        return movies.stream()
                .map(movieMapper::toResponse)
                .toList();
    }
}
