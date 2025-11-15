package com.example.cinemaster.service;




import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.util.ChatFormatter;
import com.example.cinemaster.util.SimpleCache;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.example.cinemaster.dto.response.MovieRecommendResponse;




import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;




import static com.example.cinemaster.util.ChatFormatter.*;




@Service
public class ContextRetrieverService {




    private final BranchService branchService;
    private final AuditoriumService auditoriumService;
    private final ScreeningPeriodService screeningPeriodService;
    private final ShowtimeService showtimeService;
    private final ComboService comboService;
    private final ChatSessionHistory sessionHistory;
    private final VectorStoreService vectorStoreService;
    private final MembershipLevelService membershipLevelService;
    private final NewsService newsService;
    private final MovieRecommendationService movieRecommendationService;
    private final AuthService authService;
    private final DiscountService discountService;




    // ✅ Cache 5 phút cho dữ liệu ít thay đổi
    private final SimpleCache<List<BranchResponse>> branchCache = new SimpleCache<>(5 * 60 * 1000);
    private final SimpleCache<List<Movie>> comingCache = new SimpleCache<>(5 * 60 * 1000);




    public ContextRetrieverService(
            BranchService branchService,
            AuditoriumService auditoriumService,
            ScreeningPeriodService screeningPeriodService,
            ShowtimeService showtimeService,
            ComboService comboService,
            ChatSessionHistory sessionHistory,
            VectorStoreService vectorStoreService,
            MembershipLevelService membershipLevelService,
            NewsService newsService,
            MovieRecommendationService movieRecommendationService,
            AuthService authService,
            DiscountService discountService// 👈 thêm
    ) {
        this.branchService = branchService;
        this.auditoriumService = auditoriumService;
        this.screeningPeriodService = screeningPeriodService;
        this.showtimeService = showtimeService;
        this.comboService = comboService;
        this.sessionHistory = sessionHistory;
        this.vectorStoreService = vectorStoreService;
        this.membershipLevelService = membershipLevelService;
        this.newsService = newsService;
        this.movieRecommendationService = movieRecommendationService;
        this.authService = authService;
        this.discountService = discountService;// 👈 gán
    }




    /**
     * 🧠 Trích xuất ngữ cảnh phù hợp dựa trên intent + chi nhánh + câu hỏi người dùng
     */
    public String retrieveContext(IntentRouterService.ChatIntent intent, BranchResponse targetBranch, String userInput) {
        if (targetBranch != null)
            sessionHistory.setSessionContext("target_branch", targetBranch.getBranchName());




        try {
            return switch (intent) {
                case BRANCH_INFO -> getBranchInfoContext();
                case AUDITORIUM_INFO -> getAuditoriumInfoContext(targetBranch);
                case SCREENING_NOW -> getMoviesNowShowingContext(targetBranch);
                case SCREENING_SOON -> getUpcomingMoviesContext(targetBranch);
                case SCREENING_DETAIL -> getScreeningOrShowtimeContext(userInput, targetBranch);
                case MOVIE_DETAIL -> getMovieDetailContext(userInput, targetBranch);
                case COMBO_INFO -> getComboContext(targetBranch);
                case PROMOTION_INFO -> getPromotionContext(); // ⚠️ Không có service → fallback vector
                case GENERAL_INFO -> getGeneralInfo();
                case FAQ_OR_POLICY, UNKNOWN -> retrieveVectorContext(userInput, 3);
                case MEMBERSHIP_INFO -> getMembershipLevelContext(userInput);
                case NEWS_INFO -> getNewsContext(userInput);
                case RECOMMEND_MOVIE -> getRecommendationContext(userInput);
                case MOVIE_SCREENING_BRANCH -> getMovieScreeningBranches(userInput);
                case RECOMMEND_SIMILAR -> getRecommendationContext(userInput);
                case DIRECTOR_MOVIES -> getDirectorMoviesContext(userInput);
                case CAST_MOVIES -> getCastMoviesContext(userInput);
            };
        } catch (Exception e) {
            System.err.println("⚠️ [Fallback] Lỗi SQL hoặc xử lý: " + e.getMessage());
            return "⚠️ Hệ thống đang bận, dưới đây là thông tin gợi ý từ kiến thức nền:\n"
                    + ChatFormatter.divider()
                    + retrieveVectorContext(userInput, 3);
        }
    }




    // ==========================================
    // 🔹 VECTOR RAG
    // ==========================================
    private String retrieveVectorContext(String userInput, int topK) {
        List<String> relevantDocs = vectorStoreService.searchSimilarDocuments(userInput, topK);
        if (relevantDocs.isEmpty()) {
            return emoji("💡", "Hiện tôi chưa có thông tin trong cơ sở kiến thức về câu hỏi này.");
        }
        return relevantDocs.stream()
                .map(doc -> emoji("📘", doc))
                .collect(Collectors.joining());
    }




    // ==========================================
    // 🔹 CHI NHÁNH
    // ==========================================
    private String getBranchInfoContext() {
        List<BranchResponse> branches = branchCache.get("branches", branchService::getAllActiveBranches);
        if (branches == null || branches.isEmpty())
            return emoji("🚫", "Hiện không có chi nhánh nào đang hoạt động.");




        String info = branches.stream()
                .map(b -> mdTitle("🏢 " + safeGet(b.getBranchName()))
                        + kv("Địa chỉ", b.getAddress())
                        + kv("Điện thoại", b.getPhone())
                        + kv("Giờ mở cửa", safeGet(b.getOpenTime()))
                        + kv("Giờ đóng cửa", safeGet(b.getCloseTime())))
                .collect(Collectors.joining(divider()));




        return mdTitle("📍 Danh sách chi nhánh đang hoạt động") + info;
    }




    // ==========================================
    // 🔹 PHÒNG CHIẾU
    // ==========================================
    private String getAuditoriumInfoContext(BranchResponse targetBranch) {
        if (targetBranch == null)
            return emoji("📍", "Vui lòng nói rõ chi nhánh bạn muốn xem phòng chiếu.");




        List<AuditoriumResponse> list = auditoriumService.getActiveAuditoriumsByBranchId(targetBranch.getBranchId());
        if (list.isEmpty())
            return emoji("🎞", "Chi nhánh " + targetBranch.getBranchName() + " hiện chưa có phòng chiếu hoạt động.");




        String details = list.stream()
                .map(a -> kv(a.getName(), a.getType() + " - " + a.getCapacity() + " ghế"))
                .collect(Collectors.joining());




        return mdTitle("🎬 Phòng chiếu tại " + targetBranch.getBranchName()) + details;
    }




    // ==========================================
    // 🔹 PHIM ĐANG CHIẾU
    // ==========================================
    private String getMoviesNowShowingContext(BranchResponse branch) {
        List<Movie> movies;




        if (branch == null) {
            // 🧠 Nếu user không nói chi nhánh → lấy tất cả phim đang chiếu trên toàn hệ thống
            movies = screeningPeriodService.getAllMoviesNowShowing(); // ⚙️ cần có method này trong service
            if (movies == null || movies.isEmpty())
                return emoji("🎥", "Hiện tại chưa có phim nào đang chiếu trên hệ thống CineMaster.");




            String detail = movies.stream()
                    .map(m -> {
                        String detailLink = "../movies/movieDetail.html?id=" + m.getMovieID();
                        String showtimeLink = "../user/showtimes-calendar.html?movieId=" + m.getMovieID();




                        return mdTitle("🎬 " + safeGet(m.getTitle()))
                                + kv("Đạo diễn", m.getDirector())
                                + kv("Diễn viên", m.getCast())
                                + kv("Thể loại", m.getGenre())
                                + kv("Thời lượng", safeGet(m.getDuration()) + " phút")
                                + kv("Tóm tắt", m.getDescription())
                                + "\n"
                                + ChatFormatter.link("📖 Xem chi tiết", detailLink) + " | "
                                + ChatFormatter.link("🎫 Xem suất chiếu", showtimeLink);
                    })
                    .collect(Collectors.joining(divider()));




            // 💡 Gợi ý thêm cho người dùng chọn rạp
            detail += "\n\n" + emoji("📍", "Bạn có thể hỏi thêm ví dụ: *'ở Đà Nẵng thì sao?'* để xem lịch chiếu theo rạp cụ thể nhé!");
            return mdTitle("📅 Phim đang chiếu trên toàn hệ thống CineMaster") + detail;
        }




        // 🧩 Nếu có chi nhánh cụ thể
        movies = screeningPeriodService.getMoviesNowShowingByBranchId(branch.getBranchId());
        if (movies == null || movies.isEmpty())
            return emoji("🎥", "Hiện tại không có phim nào đang chiếu ở chi nhánh " + branch.getBranchName() + ".");




        String detail = movies.stream()
                .map(m -> {
                    sessionHistory.setSessionContext("last_movie_name", m.getTitle());
                    String detailLink = "../movies/movieDetail.html?id=" + m.getMovieID();
                    String showtimeLink = "../user/showtimes-calendar.html?branchId=" + branch.getBranchId()
                            + "&movieId=" + m.getMovieID();




                    return mdTitle("🎬 " + safeGet(m.getTitle()))
                            + kv("Đạo diễn", m.getDirector())
                            + kv("Diễn viên", m.getCast())
                            + kv("Thể loại", m.getGenre())
                            + kv("Thời lượng", safeGet(m.getDuration()) + " phút")
                            + kv("Tóm tắt", m.getDescription())
                            + "\n"
                            + ChatFormatter.link("📖 Xem chi tiết", detailLink) + " | "
                            + ChatFormatter.link("🎫 Xem suất chiếu", showtimeLink);
                })
                .collect(Collectors.joining(divider()));




        return mdTitle("📅 Phim đang chiếu tại " + branch.getBranchName()) + detail;
    }
    // ==========================================
    // 🔹 PHIM SẮP CHIẾU
    // ==========================================
    private String getUpcomingMoviesContext(BranchResponse branch) {
        // 🧠 Lấy danh sách phim sắp chiếu từ cache (hoặc DB)
        List<Movie> coming = comingCache.get("comingSoon", () -> screeningPeriodService.getComingSoonMovies());
        if (coming == null || coming.isEmpty())
            return emoji("🎬", "Hiện chưa có phim sắp chiếu được công bố.");




        // 📝 Xây dựng danh sách phim chi tiết
        String detail = coming.stream()
                .map(m -> {
                    String detailLink = "../movies/movieDetail.html?id=" + m.getMovieID();
                    String showtimeLink;




                    // 🔗 Nếu có branch thì gắn branchId vào link lịch chiếu
                    if (branch != null) {
                        showtimeLink = "../user/showtimes-calendar.html?branchId=" + branch.getBranchId()
                                + "&movieId=" + m.getMovieID();
                    } else {
                        showtimeLink = "../user/showtimes-calendar.html?movieId=" + m.getMovieID();
                    }




                    return mdTitle("🎞 " + safeGet(m.getTitle()))
                            + kv("Đạo diễn", safeGet(m.getDirector()))
                            + kv("Diễn viên", safeGet(m.getCast()))
                            + kv("Thể loại", safeGet(m.getGenre()))
                            + kv("Thời lượng", safeGet(m.getDuration()) + " phút")
                            + kv("Tóm tắt", safeGet(m.getDescription()))
                            + "\n"
                            + ChatFormatter.link("📖 Xem chi tiết", detailLink) + " | "
                            + ChatFormatter.link("🎫 Đặt vé sớm", showtimeLink);
                })
                .collect(Collectors.joining(divider()));




        String branchLabel = (branch != null)
                ? " tại " + branch.getBranchName()
                : " tại CineMaster";




        return mdTitle("🎉 Phim sắp chiếu" + branchLabel) + detail;
    }








    // ==========================================
    // 🔹 KỲ CHIẾU / SUẤT CHIẾU
    // ==========================================
    private String getScreeningOrShowtimeContext(String userInput, BranchResponse branch) {
        // 🧠 Nếu user không nói chi nhánh → thử lấy từ session (dạng String)
        if (branch == null) {
            String storedBranchName = sessionHistory.getSessionContext("target_branch");
            if (storedBranchName != null) {
                branch = branchService.getAllActiveBranches().stream()
                        .filter(b -> b.getBranchName().equalsIgnoreCase(storedBranchName))
                        .findFirst()
                        .orElse(null);
            }
        }




        if (branch == null)
            return emoji("📍", "Vui lòng nói rõ chi nhánh bạn muốn xem suất chiếu.");




        // 🔍 Nếu người dùng có nhắc tới tên phim → lấy phim trực tiếp
        List<Movie> allMovies = screeningPeriodService.getAllMoviesWithPeriods();
        Movie targetMovie = allMovies.stream()
                .filter(m -> userInput.toLowerCase().contains(m.getTitle().toLowerCase()))
                .findFirst()
                .orElse(null);




        // 🧠 Nếu không tìm thấy phim trong input → lấy phim gần nhất mà user đã hỏi
        if (targetMovie == null) {
            String lastMovieName = sessionHistory.getSessionContext("last_movie_name");
            if (lastMovieName != null) {
                targetMovie = allMovies.stream()
                        .filter(m -> m.getTitle().equalsIgnoreCase(lastMovieName))
                        .findFirst()
                        .orElse(null);
            }
        }




        // ✅ Nếu xác định được phim → tạo link lịch chiếu trực tiếp
        if (targetMovie != null) {
            String link = "../user/showtimes-calendar.html?branchId=" + branch.getBranchId()
                    + "&movieId=" + targetMovie.getMovieID();
            return mdTitle("🎟 " + targetMovie.getTitle() + " tại " + branch.getBranchName())
                    + "🎫 [Xem lịch chiếu ngay](" + link + ")";
        }




        // 🗓 Nếu không có tên phim nào → hiển thị danh sách suất chiếu chung của rạp
        LocalDate date = extractDateFromInput(userInput);
        String dateText = (date != null
                ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "hôm nay");




        List<Showtime> showtimes = showtimeService.getShowtimesByBranchAndDate(branch.getBranchId(), date);
        if (showtimes.isEmpty())
            return emoji("🎟", "Không có suất chiếu nào trong " + dateText
                    + " tại chi nhánh " + branch.getBranchName() + ".");




        String showList = showtimes.stream()
                .map(s -> "• " + s.getMovie().getTitle()
                        + " — " + s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.joining("\n"));




        return mdTitle("🎟 Suất chiếu " + dateText + " tại " + branch.getBranchName()) + showList;
    }








    // ==========================================
    // 🔹 CHI TIẾT PHIM
    // ==========================================
    private String getMovieDetailContext(String userInput, BranchResponse branch) {
        List<Movie> all = screeningPeriodService.getAllMoviesWithPeriods();
        for (Movie m : all) {
            if (userInput.toLowerCase().contains(m.getTitle().toLowerCase())) {
                ScreeningPeriod p = screeningPeriodService.getCurrentPeriodByMovie(m.getMovieID());
                String periodText = (p != null)
                        ? p.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " → " +
                        p.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "Không có dữ liệu kỳ chiếu.";




                return mdTitle("🎬 " + m.getTitle())
                        + kv("Đạo diễn", m.getDirector())
                        + kv("Diễn viên", m.getCast())
                        + kv("Thể loại", m.getGenre())
                        + kv("Thời lượng", safeGet(m.getDuration()) + " phút")
                        + kv("Kỳ chiếu", periodText)
                        + kv("Mô tả", m.getDescription());
            }
        }
        return emoji("❓", "Mình chưa rõ bạn đang nói tới phim nào. Bạn có thể nhập lại tên phim nhé!");
    }




    // ==========================================
    // 🔹 COMBO (từ DB)
    // ==========================================
    private String getComboContext(BranchResponse targetBranch) {
        List<ComboResponse> combos;




        // 🔧 Dù có branch hay không, luôn cho phép lấy combo toàn hệ thống (BranchID = NULL)
        if (targetBranch != null)
            combos = comboService.getAvailableCombosByBranchId(targetBranch.getBranchId());
        else
            combos = comboService.getAvailableCombosByBranchId(null); // ✅ quan trọng




        if (combos == null || combos.isEmpty())
            return emoji("🍿", "Hiện tại chưa có combo bắp nước nào được áp dụng.");




        String comboList = combos.stream()
                .map(c -> mdTitle("🍿 " + safeGet(c.getNameCombo()))
                        + kv("Giá", String.format("%,.0f VNĐ", c.getPrice()))
                        + kv("Mô tả", safeGet(c.getDescriptionCombo()))
                        + kv("Gồm", safeGet(c.getItems()))
                        + (c.getBranchName() != null
                        ? kv("Chi nhánh", c.getBranchName())
                        : kv("Áp dụng", "Toàn hệ thống")))
                .collect(Collectors.joining(divider()));




        return mdTitle("🎁 Combo bắp nước đang bán") + comboList;
    }




    // ==========================================
    // 🔹 KHUYẾN MÃI (Fallback → Vector)
    // ==========================================
    private String getPromotionContext() {
        try {
            List<Discount> discounts = discountService.getAllActiveEntities();;
            if (discounts == null || discounts.isEmpty())
                return emoji("🎟", "Hiện tại chưa có chương trình khuyến mãi nào đang diễn ra.");




            String result = discounts.stream()
                    .map(d -> mdTitle("🎁 " + safeGet(d.getCode()))
                                    + kv("Mô tả", safeGet(d.getDiscountDescription()))
                                    + kv("Giảm giá", d.getPercentOff() != null && d.getPercentOff().compareTo(BigDecimal.ZERO) > 0
                                    ? d.getPercentOff().stripTrailingZeros().toPlainString() + "%"
                                    : d.getFixedAmount() != null
                                    ? d.getFixedAmount().stripTrailingZeros().toPlainString() + "đ" : "N/A")
                                    + kv("Ngày hết hạn", d.getExpiryDate() != null
                                    ? d.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    : "Không xác định")
                            // 👇 sửa đường dẫn tuyệt đối hơn cho frontend dễ đọc




                    )
                    .collect(Collectors.joining(divider()));




            return mdTitle("💸 Chương trình khuyến mãi tại CineMaster") + result;




        } catch (Exception e) {
            System.err.println("⚠️ [Chatbot] Không thể tải khuyến mãi: " + e.getMessage());
            return emoji("⚠️", "Hệ thống đang tạm thời không truy xuất được thông tin khuyến mãi.");
        }
    }




    // ==========================================
    // 🔹 THÔNG TIN CHUNG
    // ==========================================
    private String getGeneralInfo() {
        List<BranchResponse> branches = branchCache.get("branches", branchService::getAllActiveBranches);
        if (branches == null || branches.isEmpty())
            return emoji("📍", "Hiện CineMaster chưa có chi nhánh hoạt động.");




        return mdTitle("🗺 CineMaster hiện có " + branches.size() + " chi nhánh:")
                + branches.stream()
                .map(b -> "- " + b.getBranchName())
                .collect(Collectors.joining("\n"));
    }




    // ==========================================
    // 🔹 Tiện ích
    // ==========================================
    private String safeGet(Object value) {
        if (value == null) return "N/A";
        if (value instanceof String s) return s.isBlank() ? "N/A" : s;
        if (value instanceof List<?> list)
            return list.isEmpty() ? "N/A" : list.stream().map(Object::toString).collect(Collectors.joining(", "));
        if (value instanceof java.time.LocalTime time)
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        return value.toString();
    }




    private String getMembershipLevelContext(String userInput) {
        var page = membershipLevelService.list(Pageable.unpaged());
        List<MembershipLevelResponse> levels = page.getContent();
        if (levels == null || levels.isEmpty()) {
            return emoji("👤", "Hiện chưa có dữ liệu hạng thành viên nào trong hệ thống.");
        }




        StringBuilder sb = new StringBuilder(mdTitle("👑 Các hạng thành viên CineMaster"));
        for (MembershipLevelResponse lv : levels) {
            sb.append(mdTitle("⭐ " + safeGet(lv.getLevelName())))
                    .append(kv("Điểm yêu cầu",
                            safeGet(lv.getMinPoints()) + " - " + safeGet(lv.getMaxPoints())))
                    .append(kv("Quyền lợi", safeGet(lv.getBenefits())));
        }
        return sb.toString();
    }




    private String getNewsContext(String userInput) {
        // 🎯 Nhận diện category theo từ khóa người dùng
        String category = null;
        String lower = userInput.toLowerCase();
        if (lower.contains("khuyến mãi") || lower.contains("ưu đãi")) category = "Promotion";
        else if (lower.contains("phim") || lower.contains("ra mắt")) category = "Movie";
        else if (lower.contains("sự kiện")) category = "Event";




        List<NewsResponse> newsList = newsService.getAll(category);
        if (newsList == null || newsList.isEmpty()) {
            return emoji("📰", "Hiện chưa có tin tức mới được đăng tải.");
        }




        // 🎨 Hiển thị 5 tin mới nhất
        String detail = newsList.stream()
                .limit(5)
                .map(n -> mdTitle("🗞 " + safeGet(n.getTitle()))
                        + kv("Thể loại", safeGet(n.getCategory()))
                        + kv("Ngày đăng", n.getPublishDate() != null
                        ? n.getPublishDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "Chưa xác định")




                        + ChatFormatter.link("📖 Đọc chi tiết", "../news/listNewsCus.html?id=" + n.getNewsID()))
                .collect(Collectors.joining(divider()));




        return mdTitle("📰 Tin tức CineMaster mới nhất") + detail;
    }




    private LocalDate extractDateFromInput(String input) {
        String lower = input.toLowerCase();
        LocalDate today = LocalDate.now();
        if (Pattern.compile("hôm nay|tối nay|sáng nay|chiều nay").matcher(lower).find()) return today;
        if (Pattern.compile("ngày mai|tối mai|sáng mai|chiều mai").matcher(lower).find()) return today.plusDays(1);
        if (Pattern.compile("cuối tuần|thứ 7|chủ nhật").matcher(lower).find()) return today.plusDays(2);
        if (Pattern.compile("tuần sau").matcher(lower).find()) return today.plusWeeks(1);
        return null;
    }




    private String getRecommendationContext(String userInput) {




        Integer accountId = sessionHistory.getSessionUserId();
        boolean loggedIn = accountId != null;




        System.out.println("🧠 DEBUG | getRecommendationContext() | session_user_id = " + accountId);




        List<MovieRecommendResponse> list;




        // 🧩 1️⃣ Detect GENRE (dùng chung cho cả logged-in và guest)
        String genre = movieRecommendationService.detectGenre(userInput);
        boolean userMentionGenre = (genre != null);




        // =============================
        //  🧩 2️⃣ USER CHƯA ĐĂNG NHẬP
        // =============================
        if (!loggedIn) {




            if (userMentionGenre) {
                // ⭐ đúng theo thể loại mà user nói (Action, Romance,…)
                list = movieRecommendationService.recommendTopRatedByGenre(genre);
            } else {
                // ⭐ không có thể loại → return top hot
                list = movieRecommendationService.recommendTopRatedGlobal();
            }




            if (list.isEmpty()) {
                return emoji("🎬", "Hiện tại hệ thống chưa có dữ liệu cho thể loại này.")
                        + "\n\n📅 Bạn có thể hỏi ví dụ như:\n"
                        + "- “Gợi ý phim hành động hay nhất”\n"
                        + "- “Phim tình cảm đang được yêu thích”";
            }




            return mdTitle(userMentionGenre
                    ? "🔥 Gợi ý phim thể loại " + genre
                    : "🔥 Phim nổi bật bạn có thể thích")
                    + list.stream()
                    .map(r -> "- **" + r.getTitle() + "** (" + r.getGenre() + ") ⭐"
                            + String.format("%.1f", r.getRating() == null ? 0.0 : r.getRating())
                            + (r.getMovieId() != null
                            ? " → [Xem chi tiết](../movies/movieDetail.html?id=" + r.getMovieId() + ")"
                            : "")
                    ).collect(Collectors.joining("\n"));
        }




        // =============================
        //  🧩 3️⃣ USER ĐÃ ĐĂNG NHẬP
        // =============================




        if (userMentionGenre) {
            // ⭐ user logged-in nhưng vẫn ưu tiên thể loại nếu nói rõ
            list = movieRecommendationService.recommendTopRatedByGenre(genre);
            if (!list.isEmpty()) {
                return mdTitle("🎬 Gợi ý phim thể loại " + genre)
                        + list.stream()
                        .map(r -> "- **" + r.getTitle() + "** (" + r.getGenre() + ") ⭐"
                                + String.format("%.1f", r.getRating() == null ? 0.0 : r.getRating())
                                + " → [Xem chi tiết](../movies/movieDetail.html?id=" + r.getMovieId() + ")")
                        .collect(Collectors.joining("\n"));
            }
        }




        // ⭐ personalized recommendation
        list = movieRecommendationService.recommendForUser(accountId, userInput);




        // Không có lịch sử → global
        if (list.isEmpty()) {
            list = movieRecommendationService.recommendTopRatedGlobal();
        }




        return mdTitle("🍿 Phim dành riêng cho bạn")
                + list.stream()
                .map(r -> "- **" + r.getTitle() + "** (" + r.getGenre() + ") ⭐"
                        + String.format("%.1f", r.getRating() == null ? 0.0 : r.getRating())
                        + (r.getMovieId() != null
                        ? " → [Xem chi tiết](../movies/movieDetail.html?id=" + r.getMovieId() + ")"
                        : "")
                ).collect(Collectors.joining("\n"));
    }




    private String getMovieScreeningBranches(String userInput) {




        // 1) Tìm phim người dùng nói đến
        List<Movie> allMovies = screeningPeriodService.getAllMoviesWithPeriods();
        Movie target = allMovies.stream()
                .filter(m -> userInput.toLowerCase().contains(m.getTitle().toLowerCase()))
                .findFirst()
                .orElse(null);




        if (target == null) {
            return emoji("🎬", "Mình chưa rõ bạn đang hỏi phim nào. Bạn nhắc lại tên phim giúp mình nhé!");
        }




        // 2) Lấy danh sách chi nhánh đang chiếu phim
        List<Branch> branches = screeningPeriodService.getBranchesShowingMovie(target.getMovieID());




        if (branches.isEmpty()) {
            return emoji("📍", "Phim **" + target.getTitle() + "** hiện chưa được chiếu tại bất kỳ chi nhánh nào.");
        }




        // 3) Format kết quả
        String list = branches.stream()
                .map(b -> "- **" + b.getBranchName() + "** — " + b.getAddress())
                .collect(Collectors.joining("\n"));




        return mdTitle("🎬 Phim " + target.getTitle() + " đang chiếu tại:")
                + list;
    }
    private String getDirectorMoviesContext(String userInput) {
        String name = extractNameFromInput(userInput);
        if (name.isBlank()) {
            return emoji("🎬", "Bạn muốn xem phim của **đạo diễn nào** vậy?");
        }


        List<Movie> list = movieRecommendationService.getAllMovies().stream()
                .filter(m -> normalize(m.getDirector()).contains(normalize(name)))
                .toList();


        if (list.isEmpty()) {
            return emoji("🎥", "Không có phim nào của đạo diễn **" + name + "** trong hệ thống.");
        }


        String detail = list.stream()
                .map(m -> "- **" + m.getTitle() + "** (" + m.getGenre() + ") → "
                        + "[Xem chi tiết](../movies/movieDetail.html?id=" + m.getMovieID() + ")")
                .collect(Collectors.joining("\n"));


        return mdTitle("🎬 Phim của đạo diễn " + name) + detail;
    }
    private String getCastMoviesContext(String userInput) {
        String name = extractNameFromInput(userInput);
        if (name.isBlank()) {
            return emoji("🎬", "Bạn muốn xem phim có **diễn viên nào** vậy?");
        }


        List<Movie> list = movieRecommendationService.getAllMovies().stream()
                .filter(m -> m.getCast() != null &&
                        m.getCast().toLowerCase().contains(name.toLowerCase()))
                .toList();


        if (list.isEmpty()) {
            return emoji("🎬", "Không tìm thấy phim nào có diễn viên **" + name + "**.");
        }


        String detail = list.stream()
                .map(m -> "- **" + m.getTitle() + "** (" + m.getGenre() + ") → "
                        + "[Xem chi tiết](../movies/movieDetail.html?id=" + m.getMovieID() + ")")
                .collect(Collectors.joining("\n"));


        return mdTitle("🎬 Phim có diễn viên " + name) + detail;
    }
    private String extractNameFromInput(String input) {
        if (input == null) return "";


        String normalized = input.toLowerCase();


        // 1️⃣ Lấy tên sau "diễn viên"
        if (normalized.contains("diễn viên")) {
            return input.substring(normalized.indexOf("diễn viên") + "diễn viên".length())
                    .replaceAll("[^a-zA-ZÀ-Ỹà-ỹ\\s]", "")
                    .trim();
        }


        // 2️⃣ Lấy tên sau "đạo diễn"
        if (normalized.contains("đạo diễn")) {
            return input.substring(normalized.indexOf("đạo diễn") + "đạo diễn".length())
                    .replaceAll("[^a-zA-ZÀ-Ỹà-ỹ\\s]", "")
                    .trim();
        }


        // 3️⃣ Fallback: giữ lại ONLY chữ cái và khoảng trắng
        return input.replaceAll("[^a-zA-ZÀ-Ỹà-ỹ\\s]", "")
                .trim();
    }


    private String normalize(String text) {
        if (text == null) return "";
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }
}



