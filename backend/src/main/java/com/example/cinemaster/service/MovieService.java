package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.MovieFilterRequest;
import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.mapper.MovieMapper;
import com.example.cinemaster.repository.MovieRepository;
import jakarta.persistence.EntityNotFoundException;
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

        return movieMapper.toMovieResponse(movieRepository.save(movie));
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

        return movieMapper.toMovieResponse(movieRepository.save(movie));
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
                    return movieMapper.toMovieResponse(m);
                })
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with ID: " + id));
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
                .map(movieMapper::toMovieResponse)
                .toList();
    }

    public List<MovieResponse> filterMovies(MovieFilterRequest request) {

        // 1. Chuẩn hóa tham số: Lấy giá trị, trim (cắt khoảng trắng), và đặt thành NULL nếu rỗng.
        String title = normalizeFilterParam(request.getTitle()); // Thêm dòng này
        String genre = normalizeFilterParam(request.getGenre());
        String director = normalizeFilterParam(request.getDirector());
        String cast = normalizeFilterParam(request.getCast());
        String language = normalizeFilterParam(request.getLanguage());

        // 2. Gọi Repository với các tham số đã được chuẩn hóa.
        // Cần đảm bảo thứ tự khớp với Repository: (title, genre, director, cast, language)
        List<Movie> filteredMovies = movieRepository.findMoviesByCriteria(
                title, // Thêm title vào đầu tiên
                genre,
                director,
                cast,
                language
        );

        // 3. Chuyển đổi và trả về DTO
        return movieMapper.toMovieResponseList(filteredMovies);
    }

    /**
     * Hàm tiện ích để chuẩn hóa tham số lọc (trả về null nếu tham số rỗng hoặc null).
     */
    private String normalizeFilterParam(String param) {
        if (param == null) return null;
        String trimmed = param.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}