import { API_BASE_URL, handleResponse, getValidToken } from "./config.js";

export const ticketApi = {

    /* ==========================================================
       🎟️ 1️⃣ Tạo hoặc cập nhật vé tạm (HOLDING)
       payload = {
         accountId, showtimeId, seatIds: [...],
         combos: [...], discountIds: [...]
       }
    ========================================================== */
    async createOrUpdate(payload) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(payload),
        });
        return handleResponse(res);
    },

    /* ==========================================================
       🔍 2️⃣ Lấy vé theo ID
    ========================================================== */
    async getById(ticketId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}`, {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    /* ==========================================================
       💳 3️⃣ Xác nhận thanh toán (HOLDING → BOOKED)
       payload = {
         email: "user@gmail.com",
         combos: [{ comboId, quantity }, ...]
       }
    ========================================================== */
    async confirmPayment(ticketId, body = {}) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/confirm`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(body),
        });
        return handleResponse(res);
    },


    /* ==========================================================
       🚫 4️⃣ Hủy vé (HOLDING → CANCELLED)
    ========================================================== */
    async cancel(ticketId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/cancel`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    /* ==========================================================
       🎁 5️⃣ Áp dụng mã giảm giá
    ========================================================== */
    async applyDiscount(ticketId, code) {
        const token = localStorage.getItem("accessToken"); // 🟢 Đúng key FE của bạn
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/apply-discount/${code}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        });

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(`HTTP ${res.status}: ${err.message || res.statusText}`);
        }
        return await res.json();
    },


    /* ==========================================================
       💺 6️⃣ Lấy danh sách ghế đang giữ của vé
    ========================================================== */
    async getHeldSeats(ticketId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/seats`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    /* ==========================================================
       🔄 7️⃣ Đổi ghế trong vé HOLDING
    ========================================================== */
    async replaceSeats(ticketId, seatIds) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/seats`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(seatIds),
        });
        return handleResponse(res);
    },

    /* ==========================================================
       🧩 8️⃣ Lấy danh sách ghế đã được đặt/giữ của suất chiếu
    ========================================================== */
    async getOccupiedSeats(showtimeId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/occupied/${showtimeId}`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },
};
