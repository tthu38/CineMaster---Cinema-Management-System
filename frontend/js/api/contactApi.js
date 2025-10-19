import { API_BASE_URL, handleResponse } from './config.js';

export const contactApi = {
    async create(data) {
        const res = await fetch(`${API_BASE_URL}/contacts`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },
};
