// /js/api/showtimeApi.js
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const showtimeApi = {

    // 🟢 [POST] /api/v1/showtimes — Tạo mới lịch chiếu
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");
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

    // 🟢 [GET] /api/v1/showtimes/{id} — Lấy chi tiết lịch chiếu
    async getById(id) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 🟢 [PUT] /api/v1/showtimes/{id} — Cập nhật lịch chiếu
    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");
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

    // 🟢 [DELETE] /api/v1/showtimes/{id} — Xóa lịch chiếu
    async remove(id) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'DELETE',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/showtimes — Tìm kiếm lịch chiếu (dùng trong loadDaySlotsForAuditoriumDay)
    async search(params = {}) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");
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

    // 🟢 [GET] /api/v1/showtimes/week — Lấy lịch chiếu tuần hiện tại
    async getWeek({ anchor = null, branchId = null } = {}) {
        const token = getValidToken();
        const query = new URLSearchParams();
        if (anchor) query.set('anchor', anchor);
        if (branchId) query.set('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/showtimes/week?${query.toString()}`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/showtimes/next-week — Lấy lịch chiếu tuần kế
    async getNextWeek(branchId = null) {
        const token = getValidToken();
        const url = `${API_BASE_URL}/showtimes/next-week${branchId ? `?branchId=${branchId}` : ''}`;
        const res = await fetch(url, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },
};
