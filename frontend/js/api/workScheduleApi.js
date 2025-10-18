// ================= WORK SCHEDULE API =================
import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

async function create(payload) {
    const token = getValidToken();
    const res = await fetch(`${API_BASE_URL}/work-schedules`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
    });
    return handleResponse(res);
}

async function update(id, payload) {
    const token = getValidToken();
    const res = await fetch(`${API_BASE_URL}/work-schedules/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
    });
    return handleResponse(res);
}

async function deleteSchedule(id) {
    const token = getValidToken();
    const res = await fetch(`${API_BASE_URL}/work-schedules/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok && res.status !== 204) {
        throw new Error(`Delete failed (${res.status})`);
    }
}

async function get(id) {
    const token = getValidToken();
    const res = await fetch(`${API_BASE_URL}/work-schedules/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    return handleResponse(res);
}

async function search({ accountId, branchId, from, to, page = 0, size = 50, sort = "shiftDate,ASC" }) {
    const token = getValidToken();
    const params = new URLSearchParams();
    if (accountId) params.append("accountId", accountId);
    if (branchId) params.append("branchId", branchId);
    if (from) params.append("from", from);
    if (to) params.append("to", to);
    params.append("page", page);
    params.append("size", size);
    params.append("sort", sort);

    const res = await fetch(`${API_BASE_URL}/work-schedules?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    return handleResponse(res);
}

// ===== MATRIX (dạng Excel UI) =====
async function getMatrix({ from, to, branchId }) {
    if (!branchId) throw new Error("branchId là bắt buộc");
    const token = getValidToken();
    const url = `${API_BASE_URL}/work-schedules/matrix?from=${from}&to=${to}&branchId=${branchId}`;
    const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
    });
    return handleResponse(res);
}

async function getCell({ branchId, date, shiftType }) {
    const token = getValidToken();
    const url = `${API_BASE_URL}/work-schedules/cell?branchId=${branchId}&date=${date}&shiftType=${shiftType}`;
    const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
    });
    return handleResponse(res);
}

async function upsertCellMany(payload) {
    const token = getValidToken();
    const res = await fetch(`${API_BASE_URL}/work-schedules/upsert-cell-many`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
    });
    return handleResponse(res);
}

// ✅ Export kiểu default object
export default {
    create,
    update,
    deleteSchedule,
    get,
    search,
    getMatrix,
    getCell,
    upsertCellMany,
};