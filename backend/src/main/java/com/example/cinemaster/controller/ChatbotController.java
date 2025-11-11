package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ChatRequest;
import com.example.cinemaster.dto.response.ChatResponse;
import com.example.cinemaster.service.ChatbotService;
import jakarta.servlet.http.HttpSession; // Import mới
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Endpoint chính để gửi câu hỏi đến Chatbot AI.
     * @param request Chứa câu hỏi của người dùng (question).
     * @param session Đối tượng HttpSession để đảm bảo session được khởi tạo/duy trì. ✨ THÊM ĐIỂM NÀY
     * @return Câu trả lời từ AI (answer).
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askChatbot(@RequestBody ChatRequest request, HttpSession session) {
        System.out.println("Session ID: " + session.getId());

        String question = request.getQuestion();

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Vui lòng nhập câu hỏi để tôi có thể hỗ trợ."));
        }

        try {
            String answer = chatbotService.getChatbotResponse(question.trim());

            return ResponseEntity.ok(new ChatResponse(answer));

        } catch (Exception e) {
            System.err.println("Lỗi gọi Chatbot API: " + e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ChatResponse("Xin lỗi, hệ thống AI đang gặp sự cố. Vui lòng kiểm tra log hệ thống.")
            );
        }
    }
}