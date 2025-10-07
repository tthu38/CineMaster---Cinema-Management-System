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
const authApi = {
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
        const token = getValidToken();
        if (token) {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` },
            });
        }
        localStorage.removeItem('accessToken');
        window.location.href = '../home/home.html'; // redirect về login
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

    async requestPasswordReset(email) {
        const res = await fetch(`${API_BASE_URL}/auth/request-otp`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email }),
        });
        return handleResponse(res);
    },

    async resetPassword(email, otp, newPassword) {
        const res = await fetch(`${API_BASE_URL}/auth/reset`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, otp, newPassword }),
        });
        return handleResponse(res);
    },

    // 👇 Thêm mới hàm reset bằng token
    async resetByToken(token, newPassword) {
        const res = await fetch(`${API_BASE_URL}/auth/reset-by-token`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ token, newPassword }),
        });
        return handleResponse(res);
    },

};

// ===== Movie API public =====
export const moviesApi = {
    async getAll() {
        const res = await fetch(`${API_BASE_URL}/movies`);
        return handleResponse(res);
    },

    async getById(id) {
        const res = await fetch(`${API_BASE_URL}/movies/${id}`);
        return handleResponse(res);
    },

    async create(movieData, posterFile) {
        const formData = new FormData();
        formData.append("movie", new Blob([JSON.stringify(movieData)], { type: "application/json" })); // Fix consistency
        if (posterFile) formData.append("posterFile", posterFile);

        const res = await fetch(`${API_BASE_URL}/movies`, {
            method: "POST",
            body: formData
        });
        return handleResponse(res);
    },

    async update(id, movieData, posterFile) {
        const formData = new FormData();
        // 👇 Gửi JSON Blob thay vì string
        formData.append("movie", new Blob([JSON.stringify(movieData)], { type: "application/json" }));
        if (posterFile) {
            formData.append("posterFile", posterFile);
        }

        const res = await fetch(`${API_BASE_URL}/movies/${id}`, {
            method: "PUT",
            body: formData
        });
        return handleResponse(res);
    },

    async remove(id) {
        const res = await fetch(`${API_BASE_URL}/movies/${id}`, {
            method: 'DELETE'
        });
        return handleResponse(res);
    }
};

// ===== Export gộp =====
export const api = {
    ...authApi,
    ...userApi,
    // ...moviesApi, // Uncomment nếu cần export movies
};