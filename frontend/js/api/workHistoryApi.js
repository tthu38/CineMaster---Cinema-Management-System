import { API_BASE_URL, getValidToken, handleResponse } from "./config.js";

const _workHistoryApi = {
    // 📌 Tạo mới WorkHistory
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/work-histories`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Lấy WorkHistory theo ID
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tìm kiếm (filter + phân trang)
    async search({ accountId, affectedAccountId, from, to, page = 0, size = 20, sort = "actionTime,ASC" } = {}) {
        const token = getValidToken();
        if (!token) return null;

        const params = new URLSearchParams();
        if (accountId) params.append("accountId", accountId);
        if (affectedAccountId) params.append("affectedAccountId", affectedAccountId);
        if (from) params.append("from", from);
        if (to) params.append("to", to);
        params.append("page", page);
        params.append("size", size);
        params.append("sort", sort);

        const res = await fetch(`${API_BASE_URL}/work-histories?${params.toString()}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật WorkHistory
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa WorkHistory
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};

export const workHistoryApi = _workHistoryApi;
