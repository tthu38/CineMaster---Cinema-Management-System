
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const accountApi = {

    async getAll() {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    async getAllPaged(page = 0, size = 10, roleId = null, branchId = null, keyword = "", isActive = null) {
        const token = getValidToken();
        if (!token) return null;

        let url = `${API_BASE_URL}/accounts?page=${page}&size=${size}`;
        if (roleId) url += `&roleId=${roleId}`;
        if (branchId !== null && branchId !== undefined && branchId !== "") {
            url += `&branchId=${branchId}`;
        }
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

        // 👇 THÊM DÒNG NÀY ĐỂ LỌC THEO TRẠNG THÁI
        if (isActive !== null) url += `&isActive=${isActive}`;

        console.log("📡 Fetching:", url);
        const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
        return handleResponse(res);
    },

    async getById(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    async create(accountData, avatarFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data", // ✅ phải trùng với @RequestPart("data")
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`, // ❌ KHÔNG set Content-Type, để fetch tự gắn
            },
            body: formData,
        });
        return handleResponse(res);
    },

    async update(id, accountData, avatarFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data", // ✅ giống @RequestPart("data")
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`, // không set Content-Type
            },
            body: formData,
        });
        return handleResponse(res);
    },

    async remove(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
    async restore(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    }

};
