import { API_BASE_URL, getValidToken, handleResponse } from './config.js';

export const accountApi = {

    async getAll() {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    /**
     * getAllPaged
     * @param page
     * @param size
     * @param roleId
     * @param branchId
     * @param keyword
     * @param isActive
     * @param isActiveMode  // ⬅️ thêm mode để bật "luôn gửi rỗng"
     *      - null (default): giữ hành vi gốc của bạn
     *      - "forceEmpty": luôn gửi &isActive=
     */
    async getAllPaged(
        page = 0,
        size = 10,
        roleId = null,
        branchId = null,
        keyword = "",
        isActive = null,
        isActiveMode = null
    ) {
        const token = getValidToken();
        if (!token) return null;

        let url = `${API_BASE_URL}/accounts?page=${page}&size=${size}`;
        if (roleId) url += `&roleId=${roleId}`;
        if (branchId !== null && branchId !== undefined && branchId !== "") {
            url += `&branchId=${branchId}`;
        }
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

        // ================================
        // ⭐ GỘP LOGIC CỦA HAI VERSION ⭐
        // ================================

        if (isActiveMode === "forceEmpty") {
            // Version 2: luôn gửi rỗng
            url += `&isActive=`;
        } else {
            // Version GỐC của bạn: chỉ gửi khi isActive !== null
            if (isActive !== null) url += `&isActive=${isActive}`;
        }

        console.log("📡 Fetching:", url);

        const res = await fetch(url, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    async getById(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    async create(accountData, avatarFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data",
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    async update(id, accountData, avatarFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data",
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    async remove(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    async restore(id) {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    }

};
