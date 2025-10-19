// ==================== CONFIG IMPORT ====================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

// ==================== SHOWTIME API ====================
export const showtimeApi = {
    // 🟢 CREATE (Admin/Manager)
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để tạo lịch chiếu.");

        const res = await fetch(`${API_BASE_URL}/showtimes`, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🟡 UPDATE (Admin/Manager)
    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để cập nhật.");

        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'PUT',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🔴 DELETE (Admin/Manager)
    async remove(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để xóa.");

        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 🔍 SEARCH (lọc theo period, auditorium, from, to...)
    async search(params = {}) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để tra cứu.");

        const query = new URLSearchParams(params).toString();
        const res = await fetch(`${API_BASE_URL}/showtimes?${query}`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 🔹 GET BY ID (Public)
    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },

    // 🟢 Dành riêng cho frontend chọn ghế (không cần token)
    async getPublicById(id) {
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },

    // 📆 GET WEEK SCHEDULE (Admin/Manager có thể chọn tuần)
    async getWeek({ anchor = null, offset = 0, branchId = null } = {}) {
        let url = `${API_BASE_URL}/showtimes/week?offset=${offset}`;
        const params = [];

        if (anchor) params.push(`anchor=${encodeURIComponent(anchor)}`);
        if (branchId && branchId !== 'undefined' && branchId !== '') {
            params.push(`branchId=${encodeURIComponent(branchId)}`);
        }
        if (params.length > 0) url += `&${params.join('&')}`;

        // ✅ Thêm token vào header
        const token = getValidToken();
        const headers = {
            'Content-Type': 'application/json',
            ...(token && { Authorization: `Bearer ${token}` }),
        };

        const res = await fetch(url, { method: 'GET', headers });
        return handleResponse(res);
    },

    // 📆 GET NEXT WEEK SCHEDULE (Public)
    async getNextWeek(branchId = null) {
        const url = `${API_BASE_URL}/showtimes/next-week${branchId ? `?branchId=${branchId}` : ''}`;
        const res = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },
};
