// /js/api/branchApi.js
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const seatApi = {

    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy ghế theo ID
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo ghế mới
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật ghế
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa ghế
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo hàng loạt ghế (Bulk Create)
    async createBulk(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/bulk`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật hàng loạt ghế (Bulk Update Row)
    async bulkUpdateRow(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/bulk-update-row`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },
};
