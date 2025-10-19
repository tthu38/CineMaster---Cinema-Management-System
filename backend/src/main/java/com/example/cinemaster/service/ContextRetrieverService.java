package com.example.cinemaster.service;

import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.util.ChatFormatter;
import com.example.cinemaster.util.SimpleCache;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final MembershipLevelService  membershipLevelService;
    private final NewsService newsService;

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
            NewsService newsService // 👈 thêm
    ) {
        this.branchService = branchService;
        this.auditoriumService = auditoriumService;
        this.screeningPeriodService = screeningPeriodService;
        this.showtimeService = showtimeService;
        this.comboService = comboService;
        this.sessionHistory = sessionHistory;
        this.vectorStoreService = vectorStoreService;
        this.membershipLevelService = membershipLevelService;
        this.newsService = newsService; // 👈 gán
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
                case SCREENING_SOON -> getUpcomingMoviesContext();
                case SCREENING_DETAIL -> getScreeningOrShowtimeContext(userInput, targetBranch);
                case MOVIE_DETAIL -> getMovieDetailContext(userInput, targetBranch);
                case COMBO_INFO -> getComboContext(targetBranch);
                case PROMOTION_INFO -> getPromotionFallback(userInput); // ⚠️ Không có service → fallback vector
                case GENERAL_INFO -> getGeneralInfo();
                case FAQ_OR_POLICY, UNKNOWN -> retrieveVectorContext(userInput, 3);
                case MEMBERSHIP_INFO -> getMembershipLevelContext(userInput);
                case NEWS_INFO -> getNewsContext(userInput);
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
    private String getMoviesNowShowingContext(BranchResponse targetBranch) {
        if (targetBranch == null)
            return "Vui lòng chỉ rõ chi nhánh để tôi kiểm tra phim đang chiếu.";

        List<Movie> movies = screeningPeriodService.getMoviesNowShowingByBranchId(targetBranch.getBranchId());
        if (movies == null || movies.isEmpty())
            return emoji("🎥", "Hiện tại không có phim nào đang chiếu ở chi nhánh " + targetBranch.getBranchName() + ".");

        String detail = movies.stream()
                .map(m -> mdTitle("🎬 " + safeGet(m.getTitle()))
                        + kv("Đạo diễn", m.getDirector())
                        + kv("Diễn viên", m.getCast())
                        + kv("Thể loại", m.getGenre())
                        + kv("Thời lượng", safeGet(m.getDuration()) + " phút")
                        + kv("Tóm tắt", m.getDescription()))
                .collect(Collectors.joining(divider()));

        return mdTitle("📅 Phim đang chiếu tại " + targetBranch.getBranchName()) + detail;
    }

    // ==========================================
    // 🔹 PHIM SẮP CHIẾU
    // ==========================================
    private String getUpcomingMoviesContext() {
        List<Movie> coming = comingCache.get("comingSoon", () -> screeningPeriodService.getComingSoonMovies());
        if (coming.isEmpty())
            return emoji("🎬", "Hiện chưa có phim sắp chiếu được công bố.");
        String list = coming.stream()
                .map(m -> "🎞 " + safeGet(m.getTitle()) + " (Đạo diễn: " + safeGet(m.getDirector()) + ")")
                .collect(Collectors.joining("\n"));
        return "🎉 Phim sắp chiếu:\n" + list;
    }

    // ==========================================
    // 🔹 KỲ CHIẾU / SUẤT CHIẾU
    // ==========================================
    private String getScreeningOrShowtimeContext(String userInput, BranchResponse branch) {
        if (branch == null)
            return emoji("📅", "Vui lòng nói rõ chi nhánh bạn muốn xem suất chiếu.");

        LocalDate date = extractDateFromInput(userInput);
        String dateText = (date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "hôm nay");

        List<Showtime> showtimes = showtimeService.getShowtimesByBranchAndDate(branch.getBranchId(), date);
        if (showtimes.isEmpty())
            return emoji("🎟", "Không có suất chiếu nào trong " + dateText + " tại chi nhánh " + branch.getBranchName() + ".");

        String showList = showtimes.stream()
                .map(s -> "• " + s.getMovie().getTitle() + " — " + s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
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
    private String getPromotionFallback(String userInput) {
        return emoji("🎟", "Hiện tại hệ thống chưa tích hợp dữ liệu khuyến mãi từ DB. "
                + "Dưới đây là thông tin gợi ý từ kiến thức nền:\n")
                + retrieveVectorContext("chương trình khuyến mãi", 2);
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
        // Nếu người dùng có nói về "khuyến mãi" → lọc category tương ứng
        String category = null;
        if (userInput.toLowerCase().contains("khuyến mãi") || userInput.toLowerCase().contains("ưu đãi")) {
            category = "Promotion";
        } else if (userInput.toLowerCase().contains("phim") || userInput.toLowerCase().contains("ra mắt")) {
            category = "Movie";
        }

        // Lấy tin tức (nếu có category thì lọc, không thì lấy tất cả)
        List<NewsResponse> newsList = newsService.getAll(category);
        if (newsList == null || newsList.isEmpty()) {
            return emoji("📰", "Hiện chưa có tin tức mới được đăng tải.");
        }

        StringBuilder sb = new StringBuilder(mdTitle("📰 Tin tức CineMaster mới nhất"));
        for (NewsResponse n : newsList.stream().limit(5).toList()) { // chỉ hiển thị 5 tin đầu
            sb.append(mdTitle("📢 " + safeGet(n.getTitle())))
                    .append(kv("Thể loại", safeGet(n.getCategory())))
                    .append(kv("Ngày đăng", n.getPublishDate() != null ? n.getPublishDate().toString() : "N/A"))

                    .append(divider());
        }
        return sb.toString();
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
}
