package com.example.cinemaster.configuration;

import com.example.cinemaster.dto.request.GeminiRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.HashMap; // Import mới
import java.util.List;
import java.util.Map; // Import mới

/**
 * Bean lưu trữ lịch sử hội thoại cho mỗi Session của người dùng.
 * Sử dụng @SessionScope để duy trì trạng thái cho đến khi session kết thúc.
 */
@Component
@SessionScope
public class ChatSessionHistory {

    // Giới hạn lịch sử để tránh vượt quá Context Window của Gemini (ví dụ: 10 cặp tin nhắn gần nhất)
    private static final int MAX_HISTORY_PAIRS = 10;

    // List lưu trữ lịch sử hội thoại
    private final List<GeminiRequest.Content> history = new ArrayList<>();

    // ✨ MAP MỚI: Lưu trữ các biến ngữ cảnh ngoài lề (ví dụ: chi nhánh đích)
    private final Map<String, String> context = new HashMap<>();

    public List<GeminiRequest.Content> getHistory() {
        return history;
    }

    // --- PHƯƠNG THỨC QUẢN LÝ CONTEXT NGỮ CẢNH ---

    /**
     * Truy xuất giá trị ngữ cảnh dựa trên khóa (ví dụ: "target_branch").
     * @param key Khóa của ngữ cảnh.
     * @return Giá trị String của ngữ cảnh, hoặc null nếu không tồn tại.
     */
    public String getSessionContext(String key) {
        return context.get(key);
    }

    /**
     * Thiết lập giá trị ngữ cảnh.
     * @param key Khóa của ngữ cảnh.
     * @param value Giá trị cần lưu.
     */
    public void setSessionContext(String key, String value) {
        context.put(key, value);
    }

    /**
     * Xóa một biến ngữ cảnh cụ thể.
     * @param key Khóa cần xóa.
     */
    public void clearContext(String key) {
        context.remove(key);
    }

    // --- PHƯƠNG THỨC QUẢN LÝ LỊCH SỬ HỘI THOẠI ---

    /**
     * Thêm tin nhắn của người dùng và phản hồi của AI vào lịch sử, và giới hạn kích thước.
     * @param userQuestion Câu hỏi của người dùng
     * @param modelAnswer Phản hồi từ AI (đã trích xuất)
     */
    public void addMessage(String userQuestion, String modelAnswer) {
        // ... (Logic add message giữ nguyên) ...
        // Thêm tin nhắn người dùng
        history.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userQuestion))));

        // Thêm phản hồi của mô hình
        history.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part(modelAnswer))));

        // Giới hạn lịch sử: Nếu tổng số Content (cặp) vượt quá giới hạn, loại bỏ cặp cũ nhất
        if (history.size() > MAX_HISTORY_PAIRS * 2) {
            // Loại bỏ 2 phần tử đầu tiên (cặp tin nhắn cũ nhất: user và model)
            history.remove(0);
            history.remove(0);
        }
    }

    /**
     * Xóa toàn bộ lịch sử hội thoại và ngữ cảnh.
     */
    public void clearHistory() {
        history.clear();
        context.clear(); // Xóa cả context khi xóa lịch sử
    }
}