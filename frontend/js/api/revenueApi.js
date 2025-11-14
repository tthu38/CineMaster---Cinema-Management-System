import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const revenueApi = {

    async getLast7Days(branchId = null) {
        const token = getValidToken();
        if (!token) throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        const params = new URLSearchParams();
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/last7days?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByShift(date = null, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        if (date) params.append('anchorDate', date);
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/by-shift?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByDay(date = null, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        if (date) params.append('anchorDate', date);  // ✅ sửa lại đúng tên param
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/by-day?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByMonth(year = null, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        if (year) params.append('year', year);
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/by-month?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByYear(fromYear = null, toYear = null, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        if (fromYear) params.append('fromYear', fromYear);
        if (toYear) params.append('toYear', toYear);
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/by-year?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByMonthDetail(year, month, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        params.append('year', year);
        params.append('month', month);
        if (branchId) params.append('branchId', branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/by-month-detail?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },

    async getByCustomRange(from, to, branchId = null) {
        const token = getValidToken();
        const params = new URLSearchParams();
        params.append("from", from);
        params.append("to", to);
        if (branchId) params.append("branchId", branchId);

        const res = await fetch(`${API_BASE_URL}/revenue/custom-range?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    },


    async getTopMovies(branchId = null, filters = {}) {
        const token = getValidToken();
        const params = new URLSearchParams();
        if (branchId) params.append('branchId', branchId);
        if (filters.from) params.append('from', filters.from);
        if (filters.to) params.append('to', filters.to);
        if (filters.year) params.append('year', filters.year);
        if (filters.month) params.append('month', filters.month);

        const res = await fetch(`${API_BASE_URL}/revenue/top-movies?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`
            }
        });
        return handleResponse(res);
    }
};
