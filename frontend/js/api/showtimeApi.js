import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const showtimeApi = {

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

    async remove(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để xóa.");
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

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

    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },

    async getWeek({ anchor = null, branchId = null } = {}) {
        let url = `${API_BASE_URL}/showtimes/week`;
        const params = [];

        if (anchor) params.push(`anchor=${encodeURIComponent(anchor)}`);
        // ✅ chỉ thêm khi có giá trị thực
        if (branchId && branchId !== 'undefined' && branchId !== '') {
            params.push(`branchId=${encodeURIComponent(branchId)}`);
        }

        if (params.length > 0) url += `?${params.join('&')}`;

        const res = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },


    async getNextWeek(branchId = null) {
        const url = `${API_BASE_URL}/showtimes/next-week${branchId ? `?branchId=${branchId}` : ''}`;
        const res = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        return handleResponse(res);
    },
};
