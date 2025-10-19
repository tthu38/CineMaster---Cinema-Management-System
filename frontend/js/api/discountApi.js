import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const discountApi = {

    async getAll() {
        const res = await fetch(`${API_BASE_URL}/discounts`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/discounts/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async getByStatus(status) {
        const res = await fetch(`${API_BASE_URL}/discounts/status/${status}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },

    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("🔒 Bạn cần đăng nhập để tạo discount!");
        console.log("📦 Gửi dữ liệu tạo discount:", data);

        const res = await fetch(`${API_BASE_URL}/discounts`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });

        if (res.status === 403) throw new Error("🚫 Bạn không có quyền tạo discount!");
        return handleResponse(res);
    },

    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("🔒 Bạn cần đăng nhập để chỉnh sửa discount!");
        console.log("✏️ Cập nhật discount:", { id, ...data });

        const res = await fetch(`${API_BASE_URL}/discounts/${id}`, {
            method: "PUT",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });

        if (res.status === 403) throw new Error("🚫 Bạn không có quyền chỉnh sửa discount!");
        return handleResponse(res);
    },

    async softDelete(id) {
        const token = getValidToken();
        if (!token) throw new Error("🔒 Bạn cần đăng nhập để xóa discount!");

        const res = await fetch(`${API_BASE_URL}/discounts/${id}/delete`, {
            method: "PUT",
            headers: { "Authorization": `Bearer ${token}` },
        });

        if (res.status === 403) throw new Error("🚫 Bạn không có quyền xóa discount!");
        return handleResponse(res);
    },

    async restore(id) {
        const token = getValidToken();
        if (!token) throw new Error("🔒 Bạn cần đăng nhập để khôi phục discount!");

        const res = await fetch(`${API_BASE_URL}/discounts/${id}/restore`, {
            method: "PUT",
            headers: { "Authorization": `Bearer ${token}` },
        });

        if (res.status === 403) throw new Error("🚫 Bạn không có quyền khôi phục discount!");
        return handleResponse(res);
    },

    async hardDelete(id) {
        const token = getValidToken();
        if (!token) throw new Error("🔒 Bạn cần đăng nhập để xóa vĩnh viễn discount!");

        const res = await fetch(`${API_BASE_URL}/discounts/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` },
        });

        if (res.status === 403) throw new Error("🚫 Bạn không có quyền xóa vĩnh viễn discount!");
        return handleResponse(res);
    },

    async applyDiscount(ticketId, code) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/apply-discount/${code}`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },



};
