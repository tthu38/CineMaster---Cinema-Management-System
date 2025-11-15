package com.example.cinemaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final String API_KEY = "AIzaSyANCm-TjcrQ69iL5SffZNtyn3ELT-1mwNc";
    private final RestTemplate rest = new RestTemplate();

    public boolean isSpam(String text) {

        try {
            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                            + API_KEY;

            Map<String, Object> payload = new HashMap<>();

            // 🔥 Format đúng chuẩn Gemini 2.5
            Map<String, Object> userPrompt = new HashMap<>();
            userPrompt.put("role", "user");
            userPrompt.put("parts", List.of(
                    Map.of(
                            "text",
                            """
                                    Hãy phân loại nội dung bình luận bên dưới thành DUY NHẤT 1 từ:
                                                            - "spam" nếu có bất kỳ dấu hiệu nào sau:
                                                              * Tục tĩu, chửi bậy, nhạy cảm.
                                                              * Xúc phạm cá nhân hoặc tập thể.
                                                              * Nội dung tiêu cực phá hoại (ví dụ: "đừng xem", "phim dở", "không đáng xem").
                                                              * Xúi giục người khác không xem phim.
                                                              * Nội dung vô nghĩa, ký tự lộn xộn như ";;; afk; sakl".
                                                              * Bình luận rác, troll, spam lặp lại.
                                                              * Cố ý phá rating hoặc đánh giá không liên quan đến phim.
                                                            - "ok" nếu bình luận bình thường.
                                    
                                                            Trả lời đúng 1 từ: "spam" hoặc "ok".
                                    
                                                            Nội dung kiểm tra:
                            """ + text
                    )

            ));

            payload.put("contents", List.of(userPrompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = rest.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            String result = extractText(response.getBody()).toLowerCase();
            log.info("🔍 AI đánh giá: {}", result);

            return result.contains("spam");

        } catch (Exception e) {
            log.error("❌ Lỗi AI: {}", e.getMessage());
            return false;
        }
    }

    private String extractText(Object body) {
        try {
            Map resp = (Map) body;
            List candidates = (List) resp.get("candidates");
            Map cand0 = (Map) candidates.get(0);
            Map content = (Map) cand0.get("content");
            List parts = (List) content.get("parts");
            Map part0 = (Map) parts.get(0);

            return part0.get("text").toString();
        } catch (Exception e) {
            return "ok";
        }
    }
}
