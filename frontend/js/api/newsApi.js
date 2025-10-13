import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const newsApi = {

    async getAll(category = "") {
        let url = `${API_BASE_URL}/news`;
        if (category) url += `?category=${encodeURIComponent(category)}`;

        const res = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    create: async (formData) => {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news`, {
            method: "POST",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData,
        });
        return handleResponse(res);
    },


    async update(id, request, imageFile) {
        const token = getValidToken();
        const formData = new FormData();
        formData.append("data", new Blob([JSON.stringify(request)], { type: "application/json" }));
        if (imageFile) formData.append("imageFile", imageFile);

        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    async delete(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    async restore(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}/restore`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },
};
