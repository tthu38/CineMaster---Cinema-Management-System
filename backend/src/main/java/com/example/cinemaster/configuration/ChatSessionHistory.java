package com.example.cinemaster.configuration;

import com.example.cinemaster.dto.request.GeminiRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;

@Component
@SessionScope
public class ChatSessionHistory {

    private static final int MAX_HISTORY_PAIRS = 10;

    private final List<GeminiRequest.Content> history = new ArrayList<>();

    // ✨ Map lưu ngữ cảnh (có thể là String, Integer, BranchResponse,...)
    private final Map<String, Object> context = new HashMap<>();

    public List<GeminiRequest.Content> getHistory() {
        return history;
    }

    /* ======================= NGỮ CẢNH ======================= */

    @SuppressWarnings("unchecked")
    public <T> T getSessionContext(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) return null;
        if (type.isInstance(value)) return (T) value;
        return null;
    }

    public void setSessionContext(String key, Object value) {
        context.put(key, value);
    }

    public void clearContext(String key) {
        context.remove(key);
    }

    /* ======================= LỊCH SỬ HỘI THOẠI ======================= */

    public void addMessage(String userQuestion, String modelAnswer) {
        history.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userQuestion))));
        history.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part(modelAnswer))));

        if (history.size() > MAX_HISTORY_PAIRS * 2) {
            history.remove(0);
            history.remove(0);
        }
    }

    public void clearHistory() {
        history.clear();
        context.clear();
    }
    // 🟢 Giữ lại phiên bản đơn giản — lấy context dạng String
    public String getSessionContext(String key) {
        Object value = context.get(key);
        return (value != null) ? value.toString() : null;
    }

    public void setSessionUserId(Integer userId) {
        context.put("session_user_id", userId);
    }

    public Integer getSessionUserId() {
        Object value = context.get("session_user_id");
        if (value instanceof Integer i) return i;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
