    import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

    export const revenueApi = {
        async getByShift(date = null, branchId = null) {
            const token = getValidToken();
            const params = new URLSearchParams();
            if (date) params.append('date', date);
            if (branchId) params.append('branchId', branchId);

            const res = await fetch(`${API_BASE_URL}/revenue/by-shift?${params}`, {
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
            if (date) params.append('monthAnchor', date);   // ← đổi từ monthAnchor thành date
            if (branchId) params.append('branchId', branchId);

            const res = await fetch(`${API_BASE_URL}/revenue/by-day?${params}`, {
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

            const res = await fetch(`${API_BASE_URL}/revenue/by-month?${params}`, {
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

            const res = await fetch(`${API_BASE_URL}/revenue/by-year?${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`
                }
            });
            return handleResponse(res);
        }
    };
