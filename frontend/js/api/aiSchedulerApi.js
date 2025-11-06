import { API_BASE_URL, getValidToken, handleResponse } from "./config.js";

export const aiSchedulerApi = {
    // 🧠 Gọi AI tạo lịch chiếu
    async generate(branchId, date) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(
            `${API_BASE_URL}/scheduler/generate?branchId=${branchId}&date=${date}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        return handleResponse(res);
    },

    // 💾 Lưu lịch chiếu do AI tạo
    async approveAI(showtimes) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/scheduler/approve`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(showtimes),
        });
        return handleResponse(res);
    },
};
