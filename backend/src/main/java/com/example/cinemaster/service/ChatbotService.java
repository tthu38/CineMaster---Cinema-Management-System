package com.example.cinemaster.service;


import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.request.GeminiRequest;
import com.example.cinemaster.dto.response.BranchResponse;
import com.example.cinemaster.dto.response.GeminiResponse;
import com.example.cinemaster.dto.response.MovieRecommendResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.util.ChatFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


import static com.example.cinemaster.service.IntentRouterService.ChatIntent;
import static com.example.cinemaster.util.ChatFormatter.*;


@Service
public class ChatbotService {


    private static final String API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";


    private final String geminiApiKey;
    private final RestTemplate restTemplate;
    private final ChatSessionHistory sessionHistory;
    private final IntentRouterService intentRouterService;
    private final ContextRetrieverService contextRetrieverService;
    private final MovieRecommendationService movieRecommendationService;


    public ChatbotService(
            @Value("${gemini.api.key}") String apiKey,
            RestTemplate restTemplate,
            ChatSessionHistory sessionHistory,
            IntentRouterService intentRouterService,
            ContextRetrieverService contextRetrieverService,
            MovieRecommendationService movieRecommendationService
    ) {
        this.geminiApiKey = apiKey;
        this.restTemplate = restTemplate;
        this.sessionHistory = sessionHistory;
        this.intentRouterService = intentRouterService;
        this.contextRetrieverService = contextRetrieverService;
        this.movieRecommendationService = movieRecommendationService;
    }


    /**
     * 🎯 Luồng chính xử lý RAG
     */
    public String getChatbotResponse(String userInput) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();


            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof AccountPrincipal principal) {


                sessionHistory.setSessionUserId(principal.getId());
                System.out.println("🔐 USER FROM TOKEN = " + principal.getId());


            } else {
                System.out.println("🔐 No logged-in user → guest mode");
                sessionHistory.setSessionUserId(null);
            }


        } catch (Exception ex) {
            System.out.println("⚠ Không thể lấy user từ SecurityContext: " + ex.getMessage());
        }


        // Debug check
        System.out.println("🔎 CHECK USER ID = " + sessionHistory.getSessionUserId());
        try {
            ChatIntent intent = intentRouterService.determineIntent(userInput);
            System.out.println("🧩 Detected intent = " + intent);


            BranchResponse targetBranch =
                    intentRouterService.findTargetBranch(userInput, intent).orElse(null);


            // ==========================
            // 🎬 (1) MOVIE RECOMMENDATION FIXED
            // ==========================
            if (intent == ChatIntent.RECOMMEND_MOVIE) {


                Integer accountId = sessionHistory.getSessionUserId();
                String genre = movieRecommendationService.detectGenre(userInput);


                List<MovieRecommendResponse> movies;


                // 1️⃣ User có nói thể loại → ưu tiên
                if (genre != null) {
                    movies = movieRecommendationService.recommendTopRatedByGenre(genre);
                }
                // 2️⃣ User login nhưng không nói thể loại → cá nhân hóa
                else if (accountId != null) {
                    movies = movieRecommendationService.recommendForUser(accountId, userInput);
                }
                // 3️⃣ Guest không nói thể loại → top global
                else {
                    movies = movieRecommendationService.recommendTopRatedGlobal();
                }


                // Không có dữ liệu
                if (movies.isEmpty()) {
                    return emoji("🎬", "Hiện tại hệ thống chưa có dữ liệu đánh giá để gợi ý phim.");
                }


                // Format output
                StringBuilder sb = new StringBuilder(mdTitle("🔥 Gợi ý phim dành cho bạn"));
                movies.stream()
                        .limit(5)
                        .forEach(r -> sb.append("\n• **")
                                .append(r.getTitle())
                                .append("** (").append(r.getGenre()).append(") ⭐")
                                .append(String.format("%.1f", r.getRating() == null ? 0.0 : r.getRating()))
                                .append(" → [Xem chi tiết](../movies/movieDetail.html?id=")
                                .append(r.getMovieId()).append(")"));


                return sb.toString();
            }


            // 🟧 2️⃣ Xử lý context cho intent khác
            String contextData = contextRetrieverService.retrieveContext(intent, targetBranch, userInput);


            // ⚠️ Nếu context rỗng → kiểm tra xem người dùng có đang hỏi phim từ danh sách gợi ý không
            if (contextData == null || contextData.isBlank()) {
                // Nếu là hỏi chi tiết phim
                if (intent == ChatIntent.SCREENING_DETAIL || intent == ChatIntent.MOVIE_DETAIL) {
                    var topMovies = movieRecommendationService.recommendTopRatedGlobal();
                    var matched = topMovies.stream()
                            .filter(m -> userInput.toLowerCase().contains(m.getTitle().toLowerCase()))
                            .findFirst();


                    if (matched.isPresent()) {
                        var m = matched.get();
                        return "🎬 Bộ phim **" + m.getTitle() + "** (" + m.getGenre() + ") hiện **chưa có lịch chiếu**, "
                                + "nhưng từng được khán giả đánh giá cao ⭐" + String.format("%.1f", m.getRating()) + ".\n\n"
                                + "Bạn có thể xem thêm các phim tương tự trong thể loại **" + m.getGenre() + "**:\n"
                                + buildSimilarList(m.getGenre());
                    }
                }


                contextData = "Hiện hệ thống chưa có dữ liệu cụ thể cho yêu cầu này.";
            }


            // 🧩 3️⃣ Tạo system prompt cho Gemini
            String systemPrompt = buildSystemPrompt(contextData);


            // 🧠 4️⃣ Gọi Gemini API
            String answer = callGeminiApi(systemPrompt, userInput);


            // 💾 5️⃣ Lưu lịch sử hội thoại
            sessionHistory.addMessage(userInput, answer);
            if (sessionHistory.getHistory().size() > 8) {
                sessionHistory.getHistory().remove(0);
            }


            return answer;


        } catch (Exception e) {
            System.err.println("⚠ [ChatbotService] Lỗi: " + e.getMessage());
            e.printStackTrace();
            return emoji("⚠", "Xin lỗi, tôi gặp sự cố khi kết nối với hệ thống AI. Vui lòng thử lại sau!");
        }
    }


    /**
     * 🔧 Xây prompt cho Gemini
     */
    private String buildSystemPrompt(String contextData) {
        return String.join("\n",
                "Bạn là trợ lý ảo **CineMaster**, chuyên hỗ trợ khách hàng về lịch chiếu, chi nhánh, phim và chính sách rạp.",
                "Hãy trả lời **ngắn gọn, rõ ràng, lịch sự và bằng tiếng Việt**.",
                "QUY TẮC TRẢ LỜI:",
                "- Luôn dùng thông tin trong phần DỮ LIỆU CỦA HỆ THỐNG (nếu có).",
                "- Nếu thông tin nào hiển thị là `N/A`, hãy nói rằng thông tin đó hiện chưa có trong hệ thống.",
                "- Nếu dữ liệu rỗng hoặc không liên quan, có thể trả lời chung chung (không bịa).",
                "- Giữ định dạng Markdown rõ ràng (dùng **bold**, danh sách, emoji nếu phù hợp).",
                "- ĐẶC BIỆT: Luôn giữ nguyên các liên kết Markdown dạng [Tên liên kết](URL), không được xóa hoặc rút gọn phần URL.",
                "",
                ChatFormatter.divider(),
                "**Ngày hiện tại:** " + LocalDate.now(),
                mdTitle("DỮ LIỆU CỦA HỆ THỐNG"),
                ChatFormatter.jsonBlock(contextData),
                ChatFormatter.divider()
        );
    }


    /**
     * 🔗 Gọi Gemini API
     */
    private String callGeminiApi(String systemPrompt, String userInput) {
        String apiUrl = API_BASE_URL + geminiApiKey;


        GeminiRequest.Part sysPart = new GeminiRequest.Part(systemPrompt);
        GeminiRequest.Content systemContent = new GeminiRequest.Content("system", List.of(sysPart));


        List<GeminiRequest.Content> history = sessionHistory.getHistory();
        List<GeminiRequest.Content> contents = new ArrayList<>(history);
        contents.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userInput))));


        GeminiRequest requestBody = new GeminiRequest(contents, systemContent);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);


        int maxRetries = 3;
        int retryDelay = 2000;


        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<GeminiResponse> response =
                        restTemplate.postForEntity(apiUrl, entity, GeminiResponse.class);


                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    System.out.println("✅ Gemini phản hồi thành công ở lần thử " + attempt);
                    return response.getBody().getFirstResponseText();
                }


                System.err.println("⚠ Gemini trả về mã lỗi HTTP " + response.getStatusCode());


            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("503")) {
                    System.err.println("⚠ Gemini quá tải — thử lại lần " + attempt + "/" + maxRetries);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ignored) {}
                    continue;
                }
                throw new RuntimeException("Lỗi gọi Gemini API: " + e.getMessage(), e);
            }
        }


        throw new RuntimeException("❌ Gemini API quá tải sau " + maxRetries + " lần thử. Vui lòng thử lại sau.");
    }


    /**
     * 🎥 Gợi ý danh sách phim tương tự
     */
    private String buildSimilarList(String genre) {
        List<MovieRecommendResponse> list = movieRecommendationService.recommendTopRatedByGenre(genre);
        if (list.isEmpty()) return "Không có phim cùng thể loại.";
        StringBuilder sb = new StringBuilder();
        list.stream().limit(3).forEach(r -> sb.append("• **")
                .append(r.getTitle())
                .append("** (⭐").append(String.format("%.1f", r.getRating()))
                .append(")\n"));
        return sb.toString();
    }
}

