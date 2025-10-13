// ================== SEAT API ==================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const seatApi = {

    async getAll() {
        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    async delete(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 🪑 Tạo hàng loạt ghế (Bulk Create)
    async createBulk(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/bulk`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🪑 Cập nhật hàng loạt ghế theo hàng (Bulk Update Row)
    async bulkUpdateRow(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/bulk-update-row`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },
};
