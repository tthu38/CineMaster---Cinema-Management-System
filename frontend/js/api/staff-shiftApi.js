import { API_BASE_URL, getValidToken, handleResponse } from './config.js';


export const staffShiftApi = {
    async openShift(openingCash) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/staff/shift/open?openingCash=${openingCash}`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },


    async getReport() {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/staff/shift/report`, {
            method: 'GET',
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },


    async closeShift(closingCash) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/staff/shift/close?closingCash=${closingCash}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return handleResponse(res);
    }


};

