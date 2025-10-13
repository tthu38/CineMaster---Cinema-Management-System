// ================= STAFF API =================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

async function getByBranch(branchId) {
    const token = getValidToken();
    if (!token) throw new Error("Bạn cần đăng nhập để xem danh sách nhân viên.");

    const url = `${API_BASE_URL}/staffs?branchId=${branchId}`;
    const res = await fetch(url, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    });

    return handleResponse(res);
}

export default {
    getByBranch,
};
