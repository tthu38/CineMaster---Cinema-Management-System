import { API_BASE_URL, handleResponse } from "./config.js";


// ==================== CHATBOT API ====================
export const chatApi = {


    /**
     * Gửi câu hỏi đến chatbot AI.
     * @param {string} question - Câu hỏi người dùng nhập vào.
     * @returns {Promise<string>} - Câu trả lời từ chatbot.
     */
    // async ask(question) {
    //     const res = await fetch(`${API_BASE_URL}/chat/ask`, {
    //         method: "POST",
    //         headers: { "Content-Type": "application/json" },
    //         credentials: "include",
    //         body: JSON.stringify({ question }),
    //     });
    //
    //     const data = await handleResponse(res);
    //     const answer = data.answer || data.result || data.content || data.message || data;
    //     return typeof answer === "string" ? answer : JSON.stringify(answer);
    // }
    async ask(question) {
        const token = localStorage.getItem("accessToken");


        const res = await fetch(`${API_BASE_URL}/chat/ask`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...(token ? { "Authorization": "Bearer " + token } : {})
            },
            body: JSON.stringify({ question }),
        });


        const data = await handleResponse(res);
        return data.answer;
    },
};

