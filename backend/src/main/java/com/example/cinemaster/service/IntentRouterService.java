package com.example.cinemaster.service;


import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.response.BranchResponse;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.util.Locale;


@Service
public class IntentRouterService {


    private final BranchService branchService;
    private final ChatSessionHistory sessionHistory;


    public IntentRouterService(BranchService branchService, ChatSessionHistory sessionHistory) {
        this.branchService = branchService;
        this.sessionHistory = sessionHistory;
    }


    public enum ChatIntent {
        BRANCH_INFO, AUDITORIUM_INFO, SCREENING_NOW, SCREENING_SOON,
        SCREENING_DETAIL, GENERAL_INFO, MOVIE_DETAIL,
        FAQ_OR_POLICY,COMBO_INFO, PROMOTION_INFO,MEMBERSHIP_INFO,NEWS_INFO,RECOMMEND_MOVIE,
        MOVIE_SCREENING_BRANCH,
        UNKNOWN
    }


    public ChatIntent determineIntent(String input) {
        if (input == null || input.isBlank()) return ChatIntent.UNKNOWN;
        String normalizedInput = input.toLowerCase().trim();


        boolean hasTimeReference = Pattern.compile(
                "(hôm nay|tối nay|sáng nay|chiều nay|ngày mai|tối mai|cuối tuần|tuần này|tuần sau)"
        ).matcher(normalizedInput).find();
        if (normalizedInput.matches(".*(phim .* chiếu ở đâu|chiếu ở chi nhánh nào|rạp nào đang chiếu|chiếu ở rạp nào|đang công chiếu ở đâu).*"))
            return ChatIntent.MOVIE_SCREENING_BRANCH;
        if (normalizedInput.matches(".*(suất chiếu|lịch chiếu|giờ chiếu|chiếu lúc mấy giờ|suất phim).*"))
            return ChatIntent.SCREENING_DETAIL;


        if (normalizedInput.matches(".*(phim sắp chiếu|phim sắp ra|phim mới|chuẩn bị chiếu).*"))
            return ChatIntent.SCREENING_SOON;


        if (normalizedInput.matches(".*(phim đang chiếu|đang có phim|phim nào đang|đang công chiếu).*") || hasTimeReference)
            return ChatIntent.SCREENING_NOW;


        if (normalizedInput.matches(".*(phòng chiếu|phòng nào|rạp nào|phòng vip|imax|4dx).*"))
            return ChatIntent.AUDITORIUM_INFO;


        if (normalizedInput.matches(".*(chi nhánh|địa chỉ|ở đâu|vị trí|cơ sở|rạp tại).*"))
            return ChatIntent.BRANCH_INFO;
//        if (normalizedInput.matches(".*(gợi ý phim|đề xuất phim|phim nên xem|phim gì nên xem|phim hay|phim hot|phim đang hot\\\\??|phim nổi bật|phim đang được yêu thích|phim hợp với tôi|recommend|suggest).*"))
//            return ChatIntent.RECOMMEND_MOVIE;
//        if ((normalizedInput.contains("gợi ý") && normalizedInput.contains("phim"))
//                || normalizedInput.matches(".*(đề xuất phim|phim nên xem|phim gì nên xem|phim hay|phim hot|phim nổi bật|recommend|suggest).*")) {
//            return ChatIntent.RECOMMEND_MOVIE;
//        }
        if (
            // similar movies
                normalizedInput.matches(".*(phim.*tương tự|phim.*giống như|phim.*giống|similar to|movies like|similar movies).*")
                        ||
                        // gợi ý phim nói chung
                        ((normalizedInput.contains("gợi ý") && normalizedInput.contains("phim"))
                                || normalizedInput.matches(".*(đề xuất phim|phim nên xem|phim gì nên xem|phim hay|phim hot|phim nổi bật|recommend|suggest).*"))
        ) {
            return ChatIntent.RECOMMEND_MOVIE;
        }



        if (normalizedInput.contains("khuyến mãi") || normalizedInput.contains("ưu đãi") ||
                normalizedInput.contains("giảm giá") || normalizedInput.contains("voucher")) {
            return ChatIntent.PROMOTION_INFO;
        }
        if (normalizedInput.matches(".*(chính sách|đổi vé|đổi trả|hoàn tiền|refund|mã giảm|voucher|giảm giá|ưu đãi|khuyến mãi|promotion|điểm thưởng|thẻ thành viên).*"))
            return ChatIntent.FAQ_OR_POLICY;


        if (normalizedInput.matches(".*(diễn viên|đạo diễn|thể loại|bao nhiêu phút|thời lượng|tóm tắt|mô tả|review|đánh giá).*"))
            return ChatIntent.MOVIE_DETAIL;


        if (normalizedInput.contains("combo") || normalizedInput.contains("bắp") ||
                normalizedInput.contains("nước") || normalizedInput.contains("đồ ăn")) {
            return ChatIntent.COMBO_INFO;
        }




        if (normalizedInput.matches(".*(hạn thẻ|cấp bậc|thành viên|level|hạng|điểm thưởng|ưu đãi thành viên).*"))
            return ChatIntent.MEMBERSHIP_INFO;
        if (normalizedInput.matches(".*(tin tức|news|sự kiện|khuyến mãi mới|bài viết|blog|thông báo|ra mắt phim).*"))
            return ChatIntent.NEWS_INFO;
        if (normalizedInput.matches(".*(xin chào|hello|\\\\bhi\\\\b|bạn là ai|trợ lý|hỗ trợ).*"))
            return ChatIntent.GENERAL_INFO;

        return ChatIntent.UNKNOWN;
    }


    /**
     * Xác định chi nhánh người dùng đang hỏi dựa vào nội dung hoặc session.
     */
    public Optional<BranchResponse> findTargetBranch(String userInput,ChatIntent intent) {
        if (intent == ChatIntent.RECOMMEND_MOVIE) {
            System.out.println("🔕 Skip branch matching because intent = RECOMMEND_MOVIE");
            return Optional.empty();
        }
        if (intent == ChatIntent.SCREENING_NOW ||
                intent == ChatIntent.SCREENING_SOON ||
                intent == ChatIntent.GENERAL_INFO) {

            System.out.println("🔎 Intent SCREENING_NOW / SOON không yêu cầu branch → return empty");
            return Optional.empty();
        }

        if (userInput == null || userInput.isBlank()) return Optional.empty();


        List<BranchResponse> allBranches = branchService.getAllActiveBranches();
        String normalizedInput = normalize(userInput);


        BranchResponse bestMatch = null;
        double highestScore = 0.0;


        for (BranchResponse branch : allBranches) {
            String branchNameNorm = normalize(branch.getBranchName());
            String cityName = branchNameNorm.replace("branch", "")
                    .replace("chi nhanh", "")
                    .trim();


            // =====================
            // 1️⃣ Match exact full name
            // =====================
            if (normalizedInput.contains(branchNameNorm) ||
                    normalizedInput.contains(cityName)) {
                System.out.println("🎯 Exact match: " + branch.getBranchName());
                sessionHistory.setSessionContext("target_branch", branch.getBranchName());
                return Optional.of(branch);
            }


            // =====================
            // 2️⃣ Regex boundary match — match theo từ nguyên
            // =====================
            boolean boundaryMatch = Pattern.compile("\\b" + Pattern.quote(cityName) + "\\b")
                    .matcher(normalizedInput)
                    .find();


            // =====================
            // 3️⃣ Tính độ tương đồng (Levenshtein / Jaro-Winkler đơn giản)
            // =====================
            double similarity = stringSimilarity(normalizedInput, cityName);


            // =====================
            // 4️⃣ Chấm điểm tổng hợp
            // =====================
            double score = (boundaryMatch ? 0.7 : 0.0) + (similarity * 0.3);
            if (score > highestScore && score >= 0.65) {
                highestScore = score;
                bestMatch = branch;
            }
        }


        if (bestMatch != null) {
            sessionHistory.setSessionContext("target_branch", bestMatch.getBranchName());
            System.out.println("✅ Best match branch: " + bestMatch.getBranchName() + " (score=" + highestScore + ")");
            return Optional.of(bestMatch);
        }


        // ✅ fallback: nếu có branch lưu sẵn
        String storedBranch = sessionHistory.getSessionContext("target_branch");
        if (storedBranch != null) {
            return allBranches.stream()
                    .filter(b -> b.getBranchName().equalsIgnoreCase(storedBranch))
                    .findFirst();
        }


        System.out.println("⚠️ No matching branch found for input: " + userInput);
        return Optional.empty();
    }


    /** 🔤 Chuẩn hóa tiếng Việt — bỏ dấu, lowercase, bỏ ký tự thừa */
    private String normalize(String text) {
        if (text == null) return "";
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D")
                .toLowerCase(Locale.ROOT)
                .trim();
    }


    /** 📏 Độ tương đồng chuỗi đơn giản dựa theo phần trùng */
    private double stringSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int longest = 0;
        for (int i = 0; i < a.length(); i++) {
            for (int j = 0; j < b.length(); j++) {
                int len = 0;
                while (i + len < a.length() && j + len < b.length()
                        && a.charAt(i + len) == b.charAt(j + len)) {
                    len++;
                }
                longest = Math.max(longest, len);
            }
        }
        return (double) longest / Math.max(a.length(), b.length());
    }
}

