// 📁 /js/api/showtimeApi.js
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';


export const showtimeApi = {


    // 🟢 CREATE
    async create(data) {
        const token = getValidToken();


        if (!token) {
            console.warn("🚫 Không có token trong localStorage, huỷ gửi request (CREATE).");
            alert("⚠️ Bạn chưa đăng nhập. Vui lòng đăng nhập lại trước khi tạo lịch chiếu.");
            return Promise.reject("Token missing");
        }


        const res = await fetch(`${API_BASE_URL}/showtimes`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },


    // 🟡 UPDATE
    async update(id, data) {
        if (!id) throw new Error("Thiếu ID lịch chiếu cần cập nhật.");


        const token = getValidToken();
        if (!token) {
            console.warn("🚫 Không có token trong localStorage, huỷ gửi request (UPDATE).");
            alert("⚠️ Bạn chưa đăng nhập. Vui lòng đăng nhập lại trước khi cập nhật lịch chiếu.");
            return Promise.reject("Token missing");
        }


        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: "PUT",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },


    // 🔴 DELETE
    async remove(id) {
        if (!id) throw new Error("Thiếu ID lịch chiếu cần xoá.");


        const token = getValidToken();
        if (!token) {
            console.warn("🚫 Không có token trong localStorage, huỷ gửi request (DELETE).");
            alert("⚠️ Bạn chưa đăng nhập. Vui lòng đăng nhập lại trước khi xoá lịch chiếu.");
            return Promise.reject("Token missing");
        }


        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },


    // 🔍 SEARCH
    async search(params = {}) {
        const token = getValidToken();
        const headers = { "Content-Type": "application/json" };
        if (token) headers.Authorization = `Bearer ${token}`;


        const query = new URLSearchParams(params).toString();
        const res = await fetch(`${API_BASE_URL}/showtimes?${query}`, {
            method: "GET",
            headers,
        });
        return handleResponse(res);
    },


    // 🔹 GET BY ID
    async getById(id) {
        if (!id) throw new Error("Thiếu ID lịch chiếu.");


        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },


    // 📅 GET WEEK
    async getWeek({ anchor = null, offset = 0, branchId = null, movieId = null } = {}) {
        const params = new URLSearchParams();
        if (anchor) params.append("anchor", anchor);
        if (offset) params.append("offset", offset);
        if (branchId) params.append("branchId", branchId);
        if (movieId) params.append("movieId", movieId);


        const res = await fetch(`${API_BASE_URL}/showtimes/week?${params.toString()}`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },


    // 📆 NEXT WEEK (optional helper)
    async getNextWeek(branchId = null) {
        const url = `${API_BASE_URL}/showtimes/next-week${branchId ? `?branchId=${branchId}` : ""}`;
        const res = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },
    // 📍 GET NEARBY SHOWTIMES — dùng cho định vị tự động
    async getNearby(lat, lng) {
        if (!lat || !lng) throw new Error("Thiếu toạ độ lat/lng.");


        const url = `${API_BASE_URL}/showtimes/nearby?lat=${lat}&lng=${lng}`;
        const res = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        return handleResponse(res);
    },
    // 📅 GET NEXT 7 DAYS – dành cho Viewer (Guest/Customer/Staff)
    async getNext7Days({ branchId = null, movieId = null } = {}) {
        const params = new URLSearchParams();
        if (branchId) params.append("branchId", branchId);
        if (movieId) params.append("movieId", movieId);

        const url = `${API_BASE_URL}/showtimes/next7days?${params.toString()}`;

        const res = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" }
        });

        return handleResponse(res);
    },


};

