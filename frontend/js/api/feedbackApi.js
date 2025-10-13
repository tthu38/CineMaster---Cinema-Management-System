// /js/api/branchApi.js
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const feedbackApi = {

    // 📌 Lấy danh sách feedback của phim (public)
    async getByMovie(movieId) {
        const url = `${API_BASE_URL}/feedback/movie/${movieId}`;
        const res = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 📌 Tạo feedback (yêu cầu login)
    async create(movieId, feedbackData) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn cần đăng nhập để gửi đánh giá!");

        const res = await fetch(`${API_BASE_URL}/feedback/movie/${movieId}`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(feedbackData),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật feedback (yêu cầu login)
    async update(id, feedbackData) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn cần đăng nhập để chỉnh sửa đánh giá!");

        const res = await fetch(`${API_BASE_URL}/feedback/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(feedbackData),
        });
        return handleResponse(res);
    },

    // 📌 Xóa feedback (yêu cầu login)
    async delete(id) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn cần đăng nhập để xóa đánh giá!");

        const res = await fetch(`${API_BASE_URL}/feedback/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};
