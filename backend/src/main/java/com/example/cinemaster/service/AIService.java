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
                            Hãy phân loại nội dung dưới đây thành DUY NHẤT MỘT TRONG HAI NHÃN: "spam" hoặc "ok".
                            Hãy đặc biệt khắt khe khi phân loại.  
                            Quy tắc:
                        
                            🟥 Gán "spam" nếu nội dung thuộc bất kỳ trường hợp sau:
                            - Có lời nói tục, chửi bậy, xúc phạm, miệt thị, mỉa mai, đả kích, nói xấu, vu khống.
                            - Mang tính tiêu cực mạnh, gây hấn, công kích cá nhân hoặc tập thể.
                            - Bình luận nhảm nhí, vô nghĩa, loạn ký tự, spam ký tự lập lại, hoặc không có ý nghĩa.
                            - Nội dung quảng cáo, rác, liên kết, mời chào, bán hàng, lừa đảo.
                            - Nội dung kích động thù hằn, phân biệt giới tính/chủng tộc/tôn giáo.
                            - Nội dung gợi dục, đồi trụy, 18+, ám chỉ tình dục.
                            - Lặp lại nhiều lần cùng một câu hoặc spam liên tục.
                            - Nội dung có dấu hiệu AI-generated rác không liên quan.
                            - Nội dung có ý định phá hoại, lừa đảo, troll hoặc gây phiền nhiễu.
                        
                            🟩 Gán "ok" CHỈ khi nội dung:
                            - Bình thường, lịch sự, trung lập.
                            - Có ý nghĩa, liên quan đến nội dung đang bàn.
                            - Không chứa bất kỳ yếu tố xúc phạm, tiêu cực hoặc spam.
                        
                            ❗ YÊU CẦU QUAN TRỌNG:
                            - Không giải thích.
                            - Không phân tích.
                            - Chỉ trả về đúng một từ: "spam" hoặc "ok".
                            - Hãy cực kỳ nghiêm khắc: nếu lưỡng lự → chọn "spam".
                        
                            Nội dung cần phân loại:
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

