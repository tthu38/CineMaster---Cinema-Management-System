import { API_BASE_URL, handleResponse, getValidToken } from "./config.js";


export const ticketApi = {

    async createOrUpdate(payload) {
        // 🔹 1️⃣ Lấy token hợp lệ
        const token = getValidToken();
        if (!token) {
            alert("⚠️ Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!");
            window.location.href = "../user/login.html";
            return;
        }


        // 🔹 2️⃣ Lấy email hợp lệ
        const userEmail =
            payload.customerEmail?.trim() ||
            document.getElementById("email")?.value?.trim() ||
            localStorage.getItem("userEmail") ||
            "";


        if (userEmail) localStorage.setItem("userEmail", userEmail);


        // 🔹 3️⃣ Chuẩn bị payload gửi lên backend
        const dataToSend = {
            ...payload,
            customerEmail: userEmail,
        };


        console.log("📨 [ticketApi.createOrUpdate] Gửi yêu cầu tạo/cập nhật vé:", dataToSend);


        try {
            // 🔹 4️⃣ Gửi request tới đúng API Base URL
            const res = await fetch(`${API_BASE_URL}/tickets`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
                body: JSON.stringify(dataToSend),
            });


            // 🔹 5️⃣ Nếu lỗi => log chi tiết để dễ debug
            if (!res.ok) {
                const errText = await res.text();
                console.error(`❌ Lỗi server ${res.status}:`, errText);
                throw new Error(`HTTP ${res.status}: ${errText}`);
            }


            // 🔹 6️⃣ Trả về dữ liệu JSON
            return await res.json();
        } catch (err) {
            console.error("🚨 Lỗi khi tạo vé:", err);
            alert("Không thể tạo vé. Vui lòng thử lại!");
            throw err;
        }
    },


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


    // ✅ Xác nhận thanh toán và đổi vé sang BOOKED
    async confirmPayment(ticketId, body = {}) {
        const token = getValidToken();

        const email =
            body.email ||
            document.getElementById("email")?.value?.trim() ||
            localStorage.getItem("userEmail") || null;

        const dataToSend = { ...body, email };

        console.log("💳 [ticketApi.confirmPayment] Xác nhận thanh toán vé:", dataToSend);

        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/confirm`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...(token && { Authorization: `Bearer ${token}` }),
            },
            body: JSON.stringify(dataToSend),
        });

        const json = await res.json().catch(() => ({}));

        if (!res.ok) {
            console.error("❌ Lỗi xác nhận vé:", json);
            throw new Error(json?.message || `Lỗi HTTP ${res.status}`);
        }

        // Unwrap ApiResponse (nếu có)
        return json.result || json;
    },

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


    async applyDiscount(ticketId, code) {
        const token = getValidToken();


        console.log(`🏷️ [ticketApi.applyDiscount] Áp dụng mã giảm giá ${code} cho vé ${ticketId}`);


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

    async getHeldSeats(ticketId) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/seats`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },


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

