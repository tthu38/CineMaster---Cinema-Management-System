import { API_BASE_URL, handleResponse } from "./config.js";

// ==================== CHATBOT API ====================
export const chatApi = {

    /**
     * Gửi câu hỏi đến chatbot AI.
     * @param {string} question - Câu hỏi người dùng nhập vào.
     * @returns {Promise<string>} - Câu trả lời từ chatbot.
     */
    async ask(question) {
        const res = await fetch(`${API_BASE_URL}/chat/ask`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include", // đảm bảo session hoạt động (JSESSIONID)
            body: JSON.stringify({ question }),
        });

        const data = await handleResponse(res);
        // Backend trả về ChatResponse { answer: "..." }
        return data.answer ?? data;
    },
};
