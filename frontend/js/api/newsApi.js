import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const newsApi = {

    async getAll(category = "") {
        const token = getValidToken();
        let url = `${API_BASE_URL}/news`;
        if (category) url += `?category=${encodeURIComponent(category)}`;

        const res = await fetch(url, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy tin tức theo ID
    async getById(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo tin tức mới (multipart/form-data)
    async create(formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news`, {
            method: "POST",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData, // gồm: { data: JSON Blob, imageFile }
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật tin tức (multipart/form-data)
    async update(id, formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "PUT",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    // 📌 Xóa tin tức (soft delete)
    async delete(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
        });
        return handleResponse(res);
    },

    // 📌 Khôi phục tin tức
    async restore(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}/restore`, {
            method: "PUT",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
        });
        return handleResponse(res);
    },
};

