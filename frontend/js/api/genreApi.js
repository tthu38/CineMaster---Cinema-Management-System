import { API_BASE_URL } from "./config.js";


export const genreApi = {
    async getAll() {
        const res = await fetch(`${API_BASE_URL}/movies/genres`);
        const data = await res.json();
        return data.result || [];
    }
};

