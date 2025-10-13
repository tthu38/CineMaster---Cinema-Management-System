// ================= SCREENING PERIOD API =================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const screeningPeriodApi = {
    // 📌 Tạo mới kỳ chiếu
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Lấy toàn bộ kỳ chiếu (GET /api/v1/screening-periods)
    async getAll() {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 📌 Lấy kỳ chiếu theo ID (GET /api/v1/screening-periods/{id})
    async getById(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 📌 Lấy kỳ chiếu theo Branch ID (GET /api/v1/screening-periods/branch/{branchId})
    async getByBranch(branchId) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/branch/${branchId}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 📌 Lấy kỳ chiếu đang hoạt động (GET /api/v1/screening-periods/active)
    async active({ branchId, onDate }) {
        const token = getValidToken();
        const url = new URL(`${API_BASE_URL}/screening-periods/active`);
        if (branchId) url.searchParams.set('branchId', branchId);
        if (onDate) url.searchParams.set('onDate', onDate);

        const res = await fetch(url, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },
    // 📌 Cập nhật kỳ chiếu (PUT /api/v1/screening-periods/{id})
    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa kỳ chiếu (DELETE /api/v1/screening-periods/{id})
    async delete(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};
