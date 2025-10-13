// ===============================
// 📁 /js/api/auditoriumApi.js
// ===============================
import { API_BASE_URL, getValidToken, handleResponse } from "./config.js";

export const auditoriumApi = {
    // ======================================================
    // 🔹 1. PUBLIC / STAFF — xem phòng chiếu hoạt động
    // ======================================================

    // 🟢 [GET] /api/v1/auditoriums/active
    async getAllActive() {
        const res = await fetch(`${API_BASE_URL}/auditoriums/active`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/auditoriums/{id}
    async getById(id) {
        if (!id) throw new Error("Thiếu ID phòng chiếu");
        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/auditoriums/branch/{branchId}/active
    async getActiveByBranch(branchId) {
        if (!branchId) return [];
        const res = await fetch(`${API_BASE_URL}/auditoriums/branch/${branchId}/active`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // ======================================================
    // 🔒 2. ADMIN / MANAGER — quản lý (cần token)
    // ======================================================

    // 🟢 [GET] /api/v1/auditoriums — tất cả phòng chiếu
    async getAll() {
        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/auditoriums/branch?branchId=...
    async listByBranch(branchId = null) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const url = new URL(`${API_BASE_URL}/auditoriums/branch`);
        if (branchId) url.searchParams.set("branchId", branchId);

        const res = await fetch(url, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/auditoriums/branch/{branchId}
    async getByBranch(branchId) {
        if (!branchId) return [];

        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums/branch/${branchId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 🟢 [GET] /api/v1/auditoriums/{id}/admin
    async getByIdAdmin(id) {
        if (!id) throw new Error("Thiếu ID phòng chiếu");

        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}/admin`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // ======================================================
    // 🔐 3. ADMIN — CRUD phòng chiếu
    // ======================================================

    // 🟢 [POST] /api/v1/auditoriums — tạo mới
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🟢 [PUT] /api/v1/auditoriums/{id} — cập nhật
    async update(id, data) {
        if (!id) throw new Error("Thiếu ID phòng chiếu");

        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🟠 [DELETE] /api/v1/auditoriums/{id} — xóa mềm
    async deactivate(id) {
        if (!id) throw new Error("Thiếu ID phòng chiếu");

        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 🟢 [POST] /api/v1/auditoriums/{id}/activate — kích hoạt lại
    async activate(id) {
        if (!id) throw new Error("Thiếu ID phòng chiếu");

        const token = getValidToken();
        if (!token) throw new Error("Bạn chưa đăng nhập hoặc hết phiên.");

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}/activate`, {
            method: "POST",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};
