// /js/api/showtimeApi.js
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';


export const movieApi = {


    // 📌 Lấy toàn bộ danh sách phim (public)
    async getAll(status = "") {
        let url = `${API_BASE_URL}/movies`;
        if (status) url += `?status=${encodeURIComponent(status)}`;


        const res = await fetch(url, { method: "GET" });
        return handleResponse(res);
    },


    // Lấy phim đang chiếu
    async getNowShowing() {
        const res = await fetch(`${API_BASE_URL}/movies/now-showing`);
        return handleResponse(res);
    },


// Lấy phim sắp chiếu
    async getComingSoon() {
        const res = await fetch(`${API_BASE_URL}/movies/coming-soon`);
        return handleResponse(res);
    },


    // 📌 Lấy chi tiết phim theo ID (public)
    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/movies/${id}`, { method: "GET" });
        return handleResponse(res);
    },


    // 📌 Thêm phim mới (cần token)
    async create(formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies`, {
            method: "POST",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData,
        });
        return handleResponse(res);
    },


    // 📌 Cập nhật phim (cần token)
    async update(id, formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies/${id}`, {
            method: "PUT",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData,
        });
        return handleResponse(res);
    },


    // 📌 Xóa phim (cần token)
    async delete(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
        });
        return handleResponse(res);
    },
    // filter
    async searchMovies(filterRequest) {
        // Endpoint: /api/v1/movies/search (từ MovieController của bạn)
        const params = new URLSearchParams();


        // Thêm các tham số lọc: title, genre, director, cast, language
        Object.keys(filterRequest).forEach(key => {
            const value = filterRequest[key];
            if (value) {
                // Đảm bảo mã hóa URL cho các giá trị
                params.append(key, value.trim());
            }
        });


        const url = `${API_BASE_URL}/movies/search?${params.toString()}`;


        const res = await fetch(url, { method: "GET" });
        return handleResponse(res);
    },
};

