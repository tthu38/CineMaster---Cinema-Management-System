const API_BASE_URL = 'http://localhost:8080/api/v1';

// ===== Helpers =====
function validateToken(token) {
    if (!token) return false;
    try {
        const tokenData = JSON.parse(atob(token.split('.')[1]));
        const isValid = tokenData.exp * 1000 > Date.now();
        if (!isValid) {
            localStorage.removeItem('accessToken');
        }
        return isValid;
    } catch (err) {
        console.error('Token validation error:', err);
        localStorage.removeItem('accessToken');
        return false;
    }
}

function getValidToken() {
    const token = localStorage.getItem('accessToken');
    if (!token || !validateToken(token)) return null;
    return token;
}

async function handleResponse(res) {
    const ct = res.headers.get('content-type') || '';
    let data;
    if (ct.includes('application/json')) {
        data = await res.json().catch(() => ({}));
    } else {
        data = await res.text().catch(() => '');
    }

    if (!res.ok) {
        const msg = typeof data === 'string' ? data : data.message;
        throw new Error(msg || `HTTP ${res.status}`);
    }

    // backend thường bọc data trong { code, message, result }
    return data.result ?? data;
}

// ===== Auth API =====
export const authApi = {
    async googleLogin(idToken) {
        const res = await fetch(`${API_BASE_URL}/auth/google`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ token: idToken }),
        });
        return handleResponse(res);
    },

    async login(credentials) {
        const res = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials),
        });
        return handleResponse(res);
    },

    async register(userData) {
        const res = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async logout() {
        const token = localStorage.getItem("accessToken");
        if (token) {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` },
            });
        }
        localStorage.clear();
        window.location.href = "../home/home.html";
    },
};


// ===== User API =====
const userApi = {
    async getProfile() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/users/profile`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    async updateProfile(userData) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/users/profile`, {
            method: 'PUT',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(userData),
        });
        return handleResponse(res);
    },

    async changePassword(passwordData) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/users/change-password`, {
            method: 'PUT',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(passwordData),
        });
        return handleResponse(res);
    },

    async uploadAvatar(file) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append('avatarFile', file);

        const res = await fetch(`${API_BASE_URL}/users/avatar`, {
            method: 'PUT',
            headers: {
                Authorization: `Bearer ${token}`,
                // không set Content-Type khi dùng FormData
            },
            body: formData,
        });
        return handleResponse(res);
    },

    // Gửi OTP về email mới khi user muốn đổi email
    async sendOtpChangeEmail(email) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/users/profile/send-otp-change-email`, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email }),
        });
        return handleResponse(res);
    },

    // Xác thực OTP và đổi email
    async verifyEmailChange(email, otp) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/users/profile/verify-email-change`, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, otp }),
        });
        return handleResponse(res);
    },
    // Quên mật khẩu - gửi OTP
    async requestPasswordReset(email) {
        const res = await fetch(`${API_BASE_URL}/auth/request-otp`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email }),
        });
        return handleResponse(res);
    },

    // Quên mật khẩu - xác thực OTP + đặt mật khẩu mới
    async resetPassword(email, otp, newPassword) {
        const res = await fetch(`${API_BASE_URL}/auth/reset`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, otp, newPassword }),
        });
        return handleResponse(res);
    },

};

// ===== Account API =====
const _accountApi = {
    async getAll() {
        const token = getValidToken();
        if (!token) return null;
        const res = await fetch(`${API_BASE_URL}/accounts`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        return handleResponse(res);
    },

    async getAllPaged(page = 0, size = 10, roleId = null, branchId = null, keyword = "") {
        const token = getValidToken();
        if (!token) return null;

        let url = `${API_BASE_URL}/accounts?page=${page}&size=${size}`;
        if (roleId) url += `&roleId=${roleId}`;
        if (branchId) url += `&branchId=${branchId}`;
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

        console.log("📡 Fetching:", url);
        const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
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
            "data", // ✅ phải trùng với @RequestPart("data")
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`, // ❌ KHÔNG set Content-Type, để fetch tự gắn
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
            "data", // ✅ giống @RequestPart("data")
            new Blob([JSON.stringify(accountData)], { type: "application/json" })
        );
        if (avatarFile) {
            formData.append("avatarFile", avatarFile);
        }

        const res = await fetch(`${API_BASE_URL}/accounts/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`, // không set Content-Type
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
// ============ SEAT TYPE API =================
const _seatTypeApi = {

    // 📌 Lấy danh sách loại ghế (Dùng cho dropdown)
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seattypes`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },
};
// ============ NEWS API =================
const _newsApi = {
    // 📌 Lấy toàn bộ tin tức (có thể filter theo category)
    async getAll(category = "") {
        const token = getValidToken();
        let url = `${API_BASE_URL}/news`;
        if (category) url += `?category=${encodeURIComponent(category)}`;

        const res = await fetch(url, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy tin tức theo ID
    async getById(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo tin tức mới (multipart/form-data)
    async create(formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news`, {
            method: "POST",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData, // gồm: { data: JSON Blob, imageFile }
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật tin tức (multipart/form-data)
    async update(id, formData) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "PUT",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
            body: formData,
        });
        return handleResponse(res);
    },

    // 📌 Xóa tin tức (soft delete)
    async delete(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
        });
        return handleResponse(res);
    },

    // 📌 Khôi phục tin tức
    async restore(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/news/${id}/restore`, {
            method: "PUT",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
            },
        });
        return handleResponse(res);
    },
};

export function requireAuth() {
    const token = getValidToken();
    if (!token) {
        // Nếu chưa login → quay về login
        window.location.href = "../user/login.html";
        return null;
    }
    return token;
}

// ===== Export =====
export const api = {
    ...authApi,
    ...userApi,
};

export const accountApi = _accountApi;
export const seatTypeApi = _seatTypeApi;
export const newsApi = _newsApi;


export { getValidToken, handleResponse, API_BASE_URL };

