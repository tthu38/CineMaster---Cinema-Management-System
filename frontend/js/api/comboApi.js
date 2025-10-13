// /js/api/comboApi.js
import { API_BASE_URL, getValidToken, handleResponse } from "./config.js";

export const comboApi = {
    // ===== GET ALL (admin view) =====
    async getAll() {
        const res = await fetch(`${API_BASE_URL}/combos`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // ===== GET ONLY AVAILABLE (frontend view) =====
    async getAvailable() {
        const res = await fetch(`${API_BASE_URL}/combos/available`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // ===== GET BY BRANCH =====
    async getByBranch(branchId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/combos/branch/${branchId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // ===== CREATE (multipart/form-data) =====
    async create(comboData, imageFile) {
        const token = getValidToken();
        if (!token) {
            console.error("❌ Missing token");
            return null;
        }

        const formData = new FormData();
        formData.append("data", new Blob([JSON.stringify(comboData)], { type: "application/json" }));
        if (imageFile) formData.append("imageFile", imageFile);

        console.log("📦 FormData gửi đi:", comboData);

        const res = await fetch(`${API_BASE_URL}/combos`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                // ❌ KHÔNG thêm Content-Type ở đây
            },
            body: formData,
        });

        if (!res.ok) {
            const errorText = await res.text();
            console.error("❌ API create failed:", res.status, errorText);
        }

        return handleResponse(res);
    },


    // ===== GET BY ID =====
    async getById(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // ===== UPDATE (multipart/form-data) =====
    async update(id, comboData, imageFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data",
            new Blob([JSON.stringify(comboData)], { type: "application/json" })
        );
        if (imageFile) {
            formData.append("imageFile", imageFile); // ✅ sửa lại
        }

        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    // ===== SOFT DELETE =====
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // ===== RESTORE =====
    async restore(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/combos/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};
