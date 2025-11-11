package com.example.cinemaster.service;

import com.example.cinemaster.dto.response.MovieRecommendResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.MovieFeedback;
import com.example.cinemaster.mapper.MovieMapper;
import com.example.cinemaster.repository.MovieRepository;
import com.example.cinemaster.repository.MovieFeedbackRepository;
import com.example.cinemaster.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieRecommendationService {

    private final TicketService ticketService;
    private final MovieRepository movieRepository;
    private final MovieFeedbackRepository feedbackRepo;
    private final ShowtimeRepository showtimeRepository;
    private final EmbeddingService embeddingService;

    /**
     *  Gợi ý phim dựa trên lịch sử vé của người dùng (theo thể loại)
     */
    public List<MovieRecommendResponse> recommendByTicketHistory(Integer accountId) {
        try {
            var userTickets = ticketService.getTicketsByAccount(accountId);
            if (userTickets.isEmpty()) {
                log.info(" User {} chưa có lịch sử vé.", accountId);
                return List.of();
            }

            // Lấy danh sách phim đã xem
            List<Movie> watchedMovies = userTickets.stream()
                    .map(t -> showtimeRepository.findById(t.getShowtimeId())
                            .map(s -> s.getMovie()).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();

            if (watchedMovies.isEmpty()) return List.of();

            // Tính thể loại xem nhiều nhất
            Map<String, Long> genreCount = watchedMovies.stream()
                    .collect(Collectors.groupingBy(Movie::getGenre, Collectors.counting()));
            String topGenre = genreCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Action");

            List<Movie> recommended = movieRepository.findByGenreIgnoreCase(topGenre);

            return recommended.stream()
                    .filter(m -> watchedMovies.stream().noneMatch(w -> w.getMovieID().equals(m.getMovieID())))
                    .limit(5)
                    .map(m -> MovieRecommendResponse.builder()
                            .title(m.getTitle())
                            .genre(m.getGenre())
                            .posterUrl(m.getPosterUrl())
                            .description(m.getDescription())
                            .rating(0.0)
                            .build())
                    .toList();

        } catch (Exception e) {
            log.error(" Lỗi gợi ý lịch sử vé: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     *  Gợi ý phim theo feedback người dùng
     */
    public List<MovieRecommendResponse> recommendByFeedback(Integer accountId) {
        try {
            List<MovieFeedback> allFeedbacks = feedbackRepo.findByAccount_AccountID(accountId);
            if (allFeedbacks.isEmpty()) return List.of();

            List<Movie> likedMovies = allFeedbacks.stream()
                    .filter(f -> f.getRating() >= 4)
                    .map(MovieFeedback::getMovie)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (likedMovies.isEmpty()) return List.of();

            Set<String> likedGenres = likedMovies.stream()
                    .map(Movie::getGenre)
                    .collect(Collectors.toSet());

            List<Movie> recommended = movieRepository.findAll().stream()
                    .filter(m -> likedGenres.contains(m.getGenre()))
                    .filter(m -> likedMovies.stream().noneMatch(l -> l.getMovieID().equals(m.getMovieID())))
                    .limit(5)
                    .toList();

            return recommended.stream()
                    .map(m -> MovieRecommendResponse.builder()
                            .title(m.getTitle())
                            .genre(m.getGenre())
                            .posterUrl(m.getPosterUrl())
                            .description(m.getDescription())
                            .rating(0.0)
                            .build())
                    .toList();

        } catch (Exception e) {
            log.error(" Lỗi gợi ý feedback: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     *  Gợi ý phim tương tự theo nội dung (semantic)
     */
    public List<MovieRecommendResponse> recommendSimilarMovies(String movieTitle) {
        try {
            Movie baseMovie = movieRepository.findByTitleIgnoreCase(movieTitle)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phim: " + movieTitle));

            List<Double> baseVector = embeddingService.embedText(baseMovie.getDescription());
            if (baseVector.isEmpty()) return List.of();

            List<Movie> allMovies = movieRepository.findAll();
            List<Map.Entry<Movie, Double>> scored = new ArrayList<>();

            for (Movie m : allMovies) {
                if (m.getMovieID().equals(baseMovie.getMovieID())) continue;
                List<Double> otherVector = embeddingService.embedText(m.getDescription());
                if (otherVector.isEmpty()) continue;
                double sim = cosineSimilarity(baseVector, otherVector);
                scored.add(Map.entry(m, sim));
            }

            return scored.stream()
                    .sorted(Map.Entry.<Movie, Double>comparingByValue().reversed())
                    .limit(5)
                    .map(e -> MovieRecommendResponse.builder()
                            .title(e.getKey().getTitle())
                            .genre(e.getKey().getGenre())
                            .posterUrl(e.getKey().getPosterUrl())
                            .description(e.getKey().getDescription())
                            .rating(e.getValue() * 5)
                            .build())
                    .toList();

        } catch (Exception e) {
            log.error(" Lỗi gợi ý tương tự: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     *  Gợi ý tổng hợp từ cả 3 nguồn
     */
    public List<MovieRecommendResponse> recommendHybrid(Integer accountId, String movieTitle) {
        Set<MovieRecommendResponse> finalSet = new LinkedHashSet<>();
        finalSet.addAll(recommendByTicketHistory(accountId));
        finalSet.addAll(recommendByFeedback(accountId));
        finalSet.addAll(recommendSimilarMovies(movieTitle));

        return new ArrayList<>(finalSet).stream().limit(5).toList();
    }

    // ===================== Helper =====================
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.isEmpty() || b.isEmpty() || a.size() != b.size()) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public List<MovieRecommendResponse> recommendTopRatedByGenre(String userInput) {
        try {
            String genre = detectGenreFromInput(userInput);
            List<Movie> movies = movieRepository.findAll();

            // Tính trung bình rating từng phim
            Map<Movie, Double> ratingMap = new HashMap<>();
            for (Movie m : movies) {
                var feedbacks = feedbackRepo.findByMovie_MovieID(m.getMovieID());
                double avg = feedbacks.isEmpty() ? 0.0 :
                        feedbacks.stream().mapToInt(MovieFeedback::getRating).average().orElse(0.0);
                ratingMap.put(m, avg);
            }

            // Lọc theo thể loại nếu có
            return ratingMap.entrySet().stream()
                    .filter(e -> genre == null || e.getKey().getGenre().toLowerCase().contains(genre.toLowerCase()))
                    .sorted(Map.Entry.<Movie, Double>comparingByValue().reversed())
                    .limit(5)
                    .map(e -> MovieRecommendResponse.builder()
                            .title(e.getKey().getTitle())
                            .genre(e.getKey().getGenre())
                            .posterUrl(e.getKey().getPosterUrl())
                            .description(e.getKey().getDescription())
                            .rating(e.getValue())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error(" Lỗi recommendTopRatedByGenre: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     *  Gợi ý phim có rating trung bình cao nhất (top 5)
     */
    public List<MovieRecommendResponse> recommendTopRatedGlobal() {
        try {
            List<Movie> movies = movieRepository.findAll();
            Map<Movie, Double> avgMap = new HashMap<>();
            for (Movie m : movies) {
                var feedbacks = feedbackRepo.findByMovie_MovieID(m.getMovieID());
                double avg = feedbacks.isEmpty() ? 0.0 :
                        feedbacks.stream().mapToInt(MovieFeedback::getRating).average().orElse(0.0);
                avgMap.put(m, avg);
            }

            return avgMap.entrySet().stream()
                    .sorted(Map.Entry.<Movie, Double>comparingByValue().reversed())
                    .limit(5)
                    .map(e -> MovieRecommendResponse.builder()
                            .title(e.getKey().getTitle())
                            .genre(e.getKey().getGenre())
                            .posterUrl(e.getKey().getPosterUrl())
                            .description(e.getKey().getDescription())
                            .rating(e.getValue())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error(" Lỗi recommendTopRatedGlobal: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     *  Dành cho người dùng đăng nhập — gợi ý kết hợp nhiều nguồn
     */
    public List<MovieRecommendResponse> recommendForUser(Integer accountId, String userInput) {
        // Nếu user có nói rõ tên phim → gợi ý phim tương tự
        for (Movie m : movieRepository.findAll()) {
            if (userInput.toLowerCase().contains(m.getTitle().toLowerCase())) {
                return recommendSimilarMovies(m.getTitle());
            }
        }

        // Nếu không nói rõ, kết hợp hybrid (vé + feedback + semantic)
        return recommendHybrid(accountId, "");
    }

    /**
     *  Nhận diện thể loại phim từ câu hỏi người dùng
     */
    private String detectGenreFromInput(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase();
        if (lower.contains("hành động")) return "Action";
        if (lower.contains("tình cảm") || lower.contains("romance")) return "Romance";
        if (lower.contains("kinh dị") || lower.contains("horror")) return "Horror";
        if (lower.contains("hài") || lower.contains("comedy")) return "Comedy";
        if (lower.contains("viễn tưởng") || lower.contains("sci")) return "Sci-Fi";
        if (lower.contains("hoạt hình") || lower.contains("animation")) return "Animation";
        return null;
    }
}
