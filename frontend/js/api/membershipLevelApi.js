import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const membershipLevelApi = {
    // ===== READ ALL (pageable) =====
    async list(page = 0, size = 10, sort = "id,ASC") {
        const token = getValidToken();
        const url = `${API_BASE_URL}/membership-levels?page=${page}&size=${size}&sort=${sort}`;
        const res = await fetch(url, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: "application/json",
            },
        });
        return handleResponse(res);
    },

    // ===== READ BY ID =====
    async get(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: "application/json",
            },
        });
        return handleResponse(res);
    },

    // ===== CREATE =====
    async create(payload) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/membership-levels`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(payload),
        });
        return handleResponse(res);
    },

    // ===== UPDATE =====
    async update(id, payload) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(payload),
        });
        return handleResponse(res);
    },

    // ===== DELETE =====
    async remove(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        if (res.status !== 204 && !res.ok) {
            throw new Error("❌ Xóa thất bại hoặc không có quyền.");
        }
        return true;
    },

    // ✅ alias để tương thích nếu ai gọi .delete()
    delete(id) {
        return this.remove(id);
    },
};
