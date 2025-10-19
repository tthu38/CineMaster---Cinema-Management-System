package com.example.cinemaster.service;

import com.example.cinemaster.configuration.ChatSessionHistory;
import com.example.cinemaster.dto.request.GeminiRequest;
import com.example.cinemaster.dto.response.BranchResponse;
import com.example.cinemaster.dto.response.GeminiResponse;
import com.example.cinemaster.util.ChatFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public ChatbotService(
            @Value("${gemini.api.key}") String apiKey,
            RestTemplate restTemplate,
            ChatSessionHistory sessionHistory,
            IntentRouterService intentRouterService,
            ContextRetrieverService contextRetrieverService) {
        this.geminiApiKey = apiKey;
        this.restTemplate = restTemplate;
        this.sessionHistory = sessionHistory;
        this.intentRouterService = intentRouterService;
        this.contextRetrieverService = contextRetrieverService;
    }

    /**
     * 🎯 Luồng chính xử lý RAG
     */
    public String getChatbotResponse(String userInput) {
        try {
            // 1️⃣ Intent detection
            ChatIntent intent = intentRouterService.determineIntent(userInput);
            BranchResponse targetBranch = intentRouterService.findTargetBranch(userInput).orElse(null);

            // 2️⃣ Context retrieval
            String contextData = contextRetrieverService.retrieveContext(intent, targetBranch, userInput);
            if (contextData == null || contextData.isBlank()) {
                contextData = "Hiện hệ thống chưa có dữ liệu cụ thể cho yêu cầu này.";
            }

            // 3️⃣ Build system prompt (tối ưu cho Gemini Markdown)
            String systemPrompt = buildSystemPrompt(contextData);

            // 4️⃣ Call Gemini API
            String answer = callGeminiApi(systemPrompt, userInput);

            // Lưu vào lịch sử hội thoại (chỉ giữ 8 lượt gần nhất)
            sessionHistory.addMessage(userInput, answer);
            if (sessionHistory.getHistory().size() > 8) {
                sessionHistory.getHistory().remove(0);
            }

            return answer;

        } catch (Exception e) {
            System.err.println("❌ [ChatbotService] Lỗi: " + e.getMessage());
            e.printStackTrace();
            return emoji("⚠️", "Xin lỗi, tôi gặp sự cố khi kết nối với hệ thống AI. Vui lòng thử lại sau!");
        }
    }

    /**
     * 🧠 Tạo System Prompt cho Gemini — đảm bảo Gemini hiểu đúng Markdown & context
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
                "",
                ChatFormatter.divider(),
                "**Ngày hiện tại:** " + LocalDate.now(),
                mdTitle("📂 DỮ LIỆU CỦA HỆ THỐNG"),
                ChatFormatter.jsonBlock(contextData),
                ChatFormatter.divider()
        );
    }

    /**
     * 🔗 Gọi API Gemini (với system prompt + user message + session history)
     */
    private String callGeminiApi(String systemPrompt, String userInput) {
        String apiUrl = API_BASE_URL + geminiApiKey;

        // --- System message ---
        GeminiRequest.Part sysPart = new GeminiRequest.Part(systemPrompt);
        GeminiRequest.Content systemContent = new GeminiRequest.Content("system", List.of(sysPart));

        // --- Build conversation history ---
        List<GeminiRequest.Content> history = sessionHistory.getHistory();
        List<GeminiRequest.Content> contents = new ArrayList<>(history);
        contents.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userInput))));

        GeminiRequest requestBody = new GeminiRequest(contents, systemContent);

        // --- HTTP setup ---
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);

        // --- Call API ---
        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(apiUrl, entity, GeminiResponse.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getFirstResponseText();
        }

        throw new RuntimeException("Gemini API trả về lỗi HTTP " + response.getStatusCode());
    }
}
