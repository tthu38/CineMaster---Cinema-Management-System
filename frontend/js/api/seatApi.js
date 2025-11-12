// ================== SEAT API ==================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const seatApi = {

    // 🔹 Lấy tất cả ghế (Admin hoặc sơ đồ tổng)
    async getAll() {
        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🔹 Lấy danh sách ghế theo phòng chiếu (dùng cho sơ đồ ghế)
    async getByAuditorium(auditoriumId) {
        const res = await fetch(`${API_BASE_URL}/seats/by-auditorium/${auditoriumId}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    // 🔹 Lấy ghế theo ID
    async getById(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    // 🔹 Tạo ghế đơn
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🔹 Cập nhật ghế (PATCH hoặc PUT đều ok, backend bạn đang dùng PATCH)
    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        try {
            const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(data),
            });
            return handleResponse(res);
        } catch (err) {
            console.error("❌ Lỗi khi cập nhật ghế:", err);
            throw err;
        }
    },


    // 🔹 Xóa ghế
    async delete(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // 🔹 Tạo hàng loạt ghế
    async createBulk(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/bulk`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 🔹 Cập nhật hàng loạt ghế theo dãy (đổi loại ghế / trạng thái / gộp / tách)
    async bulkUpdateRow(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");

        const res = await fetch(`${API_BASE_URL}/seats/bulk-update-row`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    async getAvailableByAuditorium(auditoriumId) {
        const res = await fetch(`${API_BASE_URL}/seats/by-auditorium/${auditoriumId}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        const allSeats = await handleResponse(res);
        return allSeats.filter(s => s.status === "AVAILABLE");
    },
};
