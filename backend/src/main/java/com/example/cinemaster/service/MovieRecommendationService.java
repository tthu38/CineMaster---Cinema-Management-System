package com.example.cinemaster.service;


import com.example.cinemaster.dto.response.MovieRecommendResponse;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.MovieFeedback;
import com.example.cinemaster.entity.Showtime;
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
                log.info("⚪ User {} chưa có lịch sử vé.", accountId);
                return List.of();
            }


            // Lấy danh sách phim đã xem
            List<Movie> watchedMovies = userTickets.stream()
                    .map(t -> {
                        Integer stId = t.getShowtimeId();
                        if (stId == null) {
                            log.warn("⚠ Ticket {} của user {} thiếu showtimeId => skip",
                                    t.getTicketId(), accountId);
                            return null;
                        }


                        return showtimeRepository.findById(stId)
                                .map(st -> st.getPeriod().getMovie())
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .toList();
            log.info(" [TicketHistory] User {} has {} tickets", accountId, userTickets.size());
            log.info(" [TicketHistory] Watched movies: {}",
                    watchedMovies.stream().map(Movie::getTitle).toList());


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
                            .movieId(m.getMovieID())
                            .title(m.getTitle())
                            .genre(m.getGenre())
                            .posterUrl(m.getPosterUrl())
                            .description(m.getDescription())
                            .rating(0.0)
                            .build())
                    .toList();




        } catch (Exception e) {
            log.error("❌ Lỗi gợi ý lịch sử vé: {}", e.getMessage());
            return List.of();
        }
    }


    /**
     * ❤️ Gợi ý phim theo feedback người dùng
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
            log.info("❤️ [Feedback] User {} liked movies: {}",
                    accountId,
                    likedMovies.stream().map(Movie::getTitle).toList());
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
                            .movieId(m.getMovieID())
                            .title(m.getTitle())
                            .genre(m.getGenre())
                            .posterUrl(m.getPosterUrl())
                            .description(m.getDescription())
                            .rating(0.0)
                            .build())
                    .toList();


        } catch (Exception e) {
            log.error("❌ Lỗi gợi ý feedback: {}", e.getMessage());
            return List.of();
        }


    }


    /**
     * 🧠 Gợi ý phim tương tự theo nội dung (semantic)
     */
//    public List<MovieRecommendResponse> recommendSimilarMovies(String movieTitle) {
//        try {
//            Movie baseMovie = movieRepository.findByTitleIgnoreCase(movieTitle)
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phim: " + movieTitle));
//
//            List<Double> baseVector = embeddingService.embedText(baseMovie.getDescription());
//            if (baseVector.isEmpty()) return List.of();
//
//            List<Movie> allMovies = movieRepository.findAll();
//            List<Map.Entry<Movie, Double>> scored = new ArrayList<>();
//
//            for (Movie m : allMovies) {
//                if (m.getMovieID().equals(baseMovie.getMovieID())) continue;
//                List<Double> otherVector = embeddingService.embedText(m.getDescription());
//                if (otherVector.isEmpty()) continue;
//                double sim = cosineSimilarity(baseVector, otherVector);
//                scored.add(Map.entry(m, sim));
//            }
//
//            return scored.stream()
//                    .sorted(Map.Entry.<Movie, Double>comparingByValue().reversed())
//                    .limit(5)
//                    .map(e -> MovieRecommendResponse.builder()
//                            .movieId(e.getKey().getMovieID())
//                            .title(e.getKey().getTitle())
//                            .genre(e.getKey().getGenre())
//                            .posterUrl(e.getKey().getPosterUrl())
//                            .description(e.getKey().getDescription())
//                            .rating(e.getValue() * 5) // quy đổi similarity thành điểm tạm
//                            .build())
//                    .toList();
//
//        } catch (Exception e) {
//            log.error("❌ Lỗi gợi ý tương tự: {}", e.getMessage());
//            return List.of();
//        }
//    }
    public List<MovieRecommendResponse> recommendSimilarMovies(String movieTitle) {
        try {
            // 1️⃣ Tìm phim gốc
            Movie baseMovie = movieRepository.findByTitleIgnoreCase(movieTitle)
                    .orElse(null);


            if (baseMovie == null) {
                log.warn("❌ Không tìm thấy phim: {}", movieTitle);
                return List.of();
            }


            String genre = baseMovie.getGenre();
            log.info("🎭 Tìm phim tương tự dựa trên thể loại: {}", genre);


            // 2️⃣ Lấy các phim cùng thể loại
            List<Object[]> rows = feedbackRepo.findTopRatedMoviesByGenreSQL(genre);


            // 3️⃣ Bỏ phim gốc ra khỏi list
            List<MovieRecommendResponse> result = rows.stream()
                    .map(r -> new MovieRecommendResponse(
                            ((Number) r[0]).intValue(),
                            (String) r[1],
                            (String) r[2],
                            ((Number) r[3]).doubleValue()
                    ))
                    .filter(r -> !r.getTitle().equalsIgnoreCase(baseMovie.getTitle()))
                    .limit(5)
                    .toList();


            return result;


        } catch (Exception e) {
            log.error("❌ Lỗi recommendSimilarMovies: {}", e.getMessage());
            return List.of();
        }
    }
    /**
     * 🤖 Gợi ý tổng hợp từ cả 3 nguồn
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
    public List<MovieRecommendResponse> recommendTopRatedByGenre(String genre) {


        if (genre == null) {
            log.info("⚪ Genre NULL — fallback global");
            return recommendTopRatedGlobal();
        }


        log.info("🎭 Gợi ý phim theo thể loại: {}", genre);


        List<Object[]> rows = feedbackRepo.findTopRatedMoviesByGenreSQL(genre);


        if (rows == null || rows.isEmpty()) {
            log.info("⚠ Không có phim nào thể loại {} — fallback global", genre);
            return recommendTopRatedGlobal();
        }


        return rows.stream()
                .map(r -> new MovieRecommendResponse(
                        ((Number) r[0]).intValue(),
                        (String) r[1],
                        (String) r[2],
                        ((Number) r[3]).doubleValue()
                ))
                .toList();
    }




    /**
     * 🌎 Gợi ý phim có rating trung bình cao nhất (top 5)
     */
    public List<MovieRecommendResponse> recommendTopRatedGlobal() {
        try {
            List<Object[]> rows = feedbackRepo.findTopRatedMoviesSQL(); // ✅ Native SQL version


            if (rows == null || rows.isEmpty()) {
                log.info("⚠ Không có dữ liệu feedback — fallback danh sách phim bất kỳ");
                return movieRepository.findAll().stream()
                        .limit(5)
                        .map(m -> MovieRecommendResponse.builder()
                                .movieId(m.getMovieID())
                                .title(m.getTitle())
                                .genre(m.getGenre())
                                .posterUrl(m.getPosterUrl())
                                .description(m.getDescription())
                                .rating(0.0)
                                .build())
                        .toList();
            }


            return rows.stream()
                    .map(r -> new MovieRecommendResponse(
                            ((Number) r[0]).intValue(),  // MovieID
                            (String) r[1],               // Title
                            (String) r[2],               // Genre
                            ((Number) r[3]).doubleValue() // Rating
                    ))
                    .limit(5)
                    .toList();


        } catch (Exception e) {
            log.error("❌ Lỗi recommendTopRatedGlobal: {}", e.getMessage());
            return List.of();
        }
    }


    /**
     * 👤 Dành cho người dùng đăng nhập — gợi ý kết hợp nhiều nguồn
     */
    public List<MovieRecommendResponse> recommendForUser(Integer accountId, String userInput) {
        log.info("🤖 [Recommender] Start recommending for user={}, input='{}'", accountId, userInput);
        try {
            // 🧩 1️⃣ Nếu user nhắc rõ thể loại
            String genre = detectGenre(userInput);
            if (genre != null) {
                log.info("🎯 User {} yêu cầu gợi ý theo thể loại: {}", accountId, genre);
                List<MovieRecommendResponse> byGenre = recommendTopRatedByGenre(genre);
                if (!byGenre.isEmpty()) return byGenre;
                log.info("🎭 [GenreDetect] Genre detected: {}", genre);
            }


            // 🧩 2️⃣ Nếu có lịch sử vé hoặc feedback — ưu tiên cá nhân hóa
            List<MovieRecommendResponse> fromTickets = recommendByTicketHistory(accountId);
            log.info("🎟 [History] Tickets found: {}", fromTickets.size());
            List<MovieRecommendResponse> fromFeedback = recommendByFeedback(accountId);
            log.info("❤️ [Feedback] Feedback entries: {}", fromFeedback.size());


            // Kết hợp 2 nguồn, bỏ trùng
            Set<MovieRecommendResponse> combined = new LinkedHashSet<>();
            combined.addAll(fromTickets);
            combined.addAll(fromFeedback);


            if (!combined.isEmpty()) {
                log.info("🍿 User {} có dữ liệu vé/feedback — dùng personalized recommendation", accountId);
                return combined.stream().limit(5).toList();
            }


            // 🧩 3️⃣ Nếu không có dữ liệu cá nhân → fallback rating toàn hệ thống
            log.info("⚪ User {} chưa có lịch sử — fallback global rating", accountId);
            return recommendTopRatedGlobal();


        } catch (Exception e) {
            log.error("❌ Lỗi recommendForUser: {}", e.getMessage());
            return recommendTopRatedGlobal();
        }
    }


    public void testTopRated() {
        var result = feedbackRepo.findTopRatedMoviesSQL(); // ✅ Native query
        System.out.println("🔥 TOP PHIM HOT:");
        result.forEach(r -> {
            System.out.println(
                    ((Number) r[0]).intValue() + " | " +   // MovieID
                            r[1] + " | " +                         // Title
                            r[2] + " | ⭐" +                       // Genre
                            ((Number) r[3]).doubleValue()          // Rating
            );
        });
    }


    /**
     * 🎭 Nhận diện thể loại phim từ câu hỏi người dùng
     */


//    public String detectGenre(String input) {
//        if (input == null) return null;
//
//        String lower = input.toLowerCase();
//
//        if (lower.contains("hành động") || lower.contains("action"))
//            return "Action";
//
//        if (lower.contains("tình cảm") || lower.contains("lãng mạn") || lower.contains("romance"))
//            return "Romance";
//
//        if (lower.contains("hài") || lower.contains("comedy"))
//            return "Comedy";
//
//        if (lower.contains("kinh dị") || lower.contains("horror"))
//            return "Horror";
//
//        if (lower.contains("viễn tưởng") || lower.contains("sci") || lower.contains("khoa học"))
//            return "Sci-Fi";
//
//        if (lower.contains("hoạt hình") || lower.contains("animation"))
//            return "Animation";
//
//        if (lower.contains("phiêu lưu") || lower.contains("adventure"))
//            return "Adventure";
//
//        return null;
//    }
    public String detectGenre(String input) {
        if (input == null || input.isBlank()) return null;


        String lower = input.toLowerCase().trim();


        // Lấy danh sách thể loại từ DB
        List<String> genres = movieRepository.findAllGenres();
        if (genres == null || genres.isEmpty()) return null;


        // 1️⃣ Match chính xác (phim Action → user nhập “action”)
        for (String g : genres) {
            if (lower.contains(g.toLowerCase())) {
                return g; // trả về đúng genre trong database
            }
        }


        // 2️⃣ Alias mapping tiếng Việt → tiếng Anh (hoặc tên genre trong DB)
        Map<String, String> alias = Map.ofEntries(
                Map.entry("hài", "Comedy"),
                Map.entry("hài hước", "Comedy"),
                Map.entry("vui", "Comedy"),
                Map.entry("cười", "Comedy"),


                Map.entry("tình cảm", "Romance"),
                Map.entry("lãng mạn", "Romance"),


                Map.entry("hành động", "Action"),
                Map.entry("đánh nhau", "Action"),


                Map.entry("kinh dị", "Horror"),
                Map.entry("ma", "Horror"),


                Map.entry("viễn tưởng", "Sci-Fi"),
                Map.entry("khoa học", "Sci-Fi"),


                Map.entry("phiêu lưu", "Adventure"),
                Map.entry("thám hiểm", "Adventure"),


                Map.entry("hoạt hình", "Animation"),
                Map.entry("anime", "Animation")
        );


        // Nếu user nhập alias → chuyển sang genre chính
        for (var entry : alias.entrySet()) {
            if (lower.contains(entry.getKey())) {


                String normalized = entry.getValue();


                // Kiểm tra normalized có tồn tại trong DB không
                for (String g : genres) {
                    if (g.equalsIgnoreCase(normalized)) {
                        return g;
                    }
                }
            }
        }


        // 3️⃣ Trường hợp user nhập sát nghĩa genre hơn DB (ví dụ “hài hước” nhưng DB lưu “Hài hước”)
        for (String g : genres) {
            if (g.toLowerCase().contains(lower)) {
                return g;
            }
        }


        return null; // không tìm được genre hợp lệ
    }
}

