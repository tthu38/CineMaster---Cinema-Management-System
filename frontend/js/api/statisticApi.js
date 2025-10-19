import { API_BASE_URL, getValidToken, handleResponse } from "../config.js";

export const statisticApi = {
    async getRevenue() {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập để xem thống kê.");

        const res = await fetch(`${API_BASE_URL}/revenue/daily`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },
};
