import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const branchApi = {

    // 🟢 CREATE (Admin)
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");

        const res = await fetch(`${API_BASE_URL}/branches`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🟢 GET ALL (Public)
    async getAll() {
        const res = await fetch(`${API_BASE_URL}/branches`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🟢 GET ACTIVE (Public)
    async getAllActive() {
        const res = await fetch(`${API_BASE_URL}/branches/active`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🟢 GET BY ID (Public)
    async getById(id) {
        if (!id) throw new Error("ID chi nhánh không hợp lệ");
        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🟢 GET BY ID (Admin)
    async getByIdAdmin(id) {
        if (!id) throw new Error("ID chi nhánh không hợp lệ");
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");

        const res = await fetch(`${API_BASE_URL}/branches/${id}/admin`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 🟡 UPDATE (Admin)
    async update(id, data) {
        if (!id) throw new Error("ID chi nhánh không hợp lệ");
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");

        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🔴 DELETE (Admin)
    async delete(id) {
        if (!id) throw new Error("ID chi nhánh không hợp lệ");
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");

        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // ♻️ RESTORE (Admin)
    async restore(id) {
        if (!id) throw new Error("ID chi nhánh không hợp lệ");
        const token = getValidToken();
        if (!token) throw new Error("Thiếu token xác thực");

        const res = await fetch(`${API_BASE_URL}/branches/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 🔹 GET NAMES (Public)
    async getNames() {
        const res = await fetch(`${API_BASE_URL}/branches/names`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },
};
