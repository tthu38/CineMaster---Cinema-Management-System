package com.example.cinemaster.service;

import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.response.BranchResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
        FAQ_OR_POLICY,COMBO_INFO, PROMOTION_INFO,MEMBERSHIP_INFO,NEWS_INFO,RECOMMEND_MOVIE, // Chính sách, giá vé, hoàn tiền
        UNKNOWN
    }

    public ChatIntent determineIntent(String input) {
        if (input == null || input.isBlank()) return ChatIntent.UNKNOWN;
        String normalizedInput = input.toLowerCase().trim();

        // 🕒 Regex mốc thời gian thông dụng
        boolean hasTimeReference = Pattern.compile(
                "(hôm nay|tối nay|sáng nay|chiều nay|ngày mai|tối mai|cuối tuần|tuần này|tuần sau)"
        ).matcher(normalizedInput).find();

        // 🎬 Suất chiếu / lịch chiếu
        if (normalizedInput.matches(".*(suất chiếu|lịch chiếu|giờ chiếu|chiếu lúc mấy giờ|suất phim).*"))
            return ChatIntent.SCREENING_DETAIL;

        // 🔮 Phim sắp chiếu
        if (normalizedInput.matches(".*(phim sắp chiếu|phim sắp ra|phim mới|chuẩn bị chiếu).*"))
            return ChatIntent.SCREENING_SOON;

        // 🎥 Phim đang chiếu (có thể kèm mốc thời gian như "tối nay")
        if (normalizedInput.matches(".*(phim đang chiếu|đang có phim|phim nào đang|đang công chiếu).*") || hasTimeReference)
            return ChatIntent.SCREENING_NOW;

        // 🏢 Phòng chiếu / rạp
        if (normalizedInput.matches(".*(phòng chiếu|phòng nào|rạp nào|phòng vip|imax|4dx).*"))
            return ChatIntent.AUDITORIUM_INFO;

        // 🏬 Chi nhánh / địa chỉ
        if (normalizedInput.matches(".*(chi nhánh|địa chỉ|ở đâu|vị trí|cơ sở|rạp tại).*"))
            return ChatIntent.BRANCH_INFO;

        // 👋 Câu chào, làm quen
        if (normalizedInput.matches(".*(xin chào|hello|hi|bạn là ai|trợ lý|hỗ trợ).*"))
            return ChatIntent.GENERAL_INFO;

        // 📜 Chính sách, đổi vé, hoàn tiền, khuyến mãi, mã giảm, combo
        if (normalizedInput.matches(".*(chính sách|đổi vé|đổi trả|hoàn tiền|refund|mã giảm|voucher|giảm giá|ưu đãi|khuyến mãi|promotion|điểm thưởng|thẻ thành viên).*"))
            return ChatIntent.FAQ_OR_POLICY;

        // 🎞️ Chi tiết phim (diễn viên, đạo diễn, thể loại, thời lượng, mô tả)
        if (normalizedInput.matches(".*(diễn viên|đạo diễn|thể loại|bao nhiêu phút|thời lượng|tóm tắt|mô tả|review|đánh giá).*"))
            return ChatIntent.MOVIE_DETAIL;

        if (normalizedInput.contains("combo") || normalizedInput.contains("bắp") ||
                normalizedInput.contains("nước") || normalizedInput.contains("đồ ăn")) {
            return ChatIntent.COMBO_INFO;
        }

        if (normalizedInput.contains("khuyến mãi") || normalizedInput.contains("ưu đãi") ||
                normalizedInput.contains("giảm giá") || normalizedInput.contains("voucher")) {
            return ChatIntent.PROMOTION_INFO;
        }
        if (normalizedInput.matches(".*(hạn thẻ|cấp bậc|thành viên|level|hạng|điểm thưởng|ưu đãi thành viên).*"))
            return ChatIntent.MEMBERSHIP_INFO;
        if (normalizedInput.matches(".*(tin tức|news|sự kiện|khuyến mãi mới|bài viết|blog|thông báo|ra mắt phim).*"))
            return ChatIntent.NEWS_INFO;
        if (normalizedInput.matches(".*(gợi ý phim|phim giống|tương tự|phim hợp với tôi|nên xem gì|recommend).*"))
            return ChatIntent.RECOMMEND_MOVIE;

        return ChatIntent.UNKNOWN;
    }

    /**
     * Xác định chi nhánh người dùng đang hỏi dựa vào nội dung hoặc session.
     */
    public Optional<BranchResponse> findTargetBranch(String userInput) {
        List<BranchResponse> allBranches = branchService.getAllActiveBranches();
        String normalizedInput = userInput.toLowerCase().trim();

        // Bước 1: Kiểm tra xem input có chứa tên chi nhánh
        for (BranchResponse branch : allBranches) {
            if (normalizedInput.contains(branch.getBranchName().toLowerCase())) {
                sessionHistory.setSessionContext("target_branch", branch.getBranchName());
                return Optional.of(branch);
            }
        }

        // Bước 2: Một số synonym phổ biến cho thành phố
        if (normalizedInput.contains("hà nội") || normalizedInput.contains("hn")) {
            return allBranches.stream().filter(b -> b.getBranchName().toLowerCase().contains("hanoi")).findFirst();
        }
        if (normalizedInput.contains("hồ chí minh") || normalizedInput.contains("sài gòn") || normalizedInput.contains("sg") || normalizedInput.contains("hcm")) {
            return allBranches.stream().filter(b -> b.getBranchName().toLowerCase().contains("hcm")).findFirst();
        }
        if (normalizedInput.contains("đà nẵng") || normalizedInput.contains("dn")) {
            return allBranches.stream().filter(b -> b.getBranchName().toLowerCase().contains("da nang") || b.getBranchName().toLowerCase().contains("dn")).findFirst();
        }


        // Bước 3: Nếu không tìm thấy, thử lấy từ session cũ
        String storedBranch = sessionHistory.getSessionContext("target_branch");
        if (storedBranch != null) {
            return allBranches.stream()
                    .filter(b -> b.getBranchName().equalsIgnoreCase(storedBranch))
                    .findFirst();
        }

        return Optional.empty();
    }


}
