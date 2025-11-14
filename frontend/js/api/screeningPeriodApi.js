import { API_BASE_URL, getValidToken, handleResponse } from './config.js';


export const screeningPeriodApi = {
    async create(data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },


    async update(id, data) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },


    async delete(id) {
        const token = getValidToken();
        if (!token) throw new Error("Vui lòng đăng nhập.");
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },


    async getAll() {
        const res = await fetch(`${API_BASE_URL}/screening-periods`);
        return handleResponse(res);
    },


    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`);
        return handleResponse(res);
    },


    async getByBranch(branchId) {
        const res = await fetch(`${API_BASE_URL}/screening-periods/branch/${branchId}`);
        return handleResponse(res);
    },


    async active({ branchId, onDate }) {
        const url = new URL(`${API_BASE_URL}/screening-periods/active`);
        if (branchId) url.searchParams.set("branchId", branchId);
        if (onDate) url.searchParams.set("onDate", onDate);


        const res = await fetch(url);
        return handleResponse(res);
    },
    async search(keyword) {
        const res = await fetch(
            `${API_BASE_URL}/screening-periods/search?keyword=${encodeURIComponent(keyword)}`,
            { method: "GET", headers: { "Content-Type": "application/json" } }
        );

        return handleResponse(res);
    },
};



