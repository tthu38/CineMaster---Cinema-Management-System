const API_BASE_URL = 'http://localhost:8080/api/v1';

// ===== Helpers =====

export function authHeaders() {// WorkHistory
    const token = getValidToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
}

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

// ===== Export gộp =====
export const api = {
    ...authApi,
    ...userApi,
};
// ===== Work History API (thêm mới) =====
export const workHistoryApi = {
    async search(params) {
        const qs = new URLSearchParams(params).toString();
        const res = await fetch(`${API_BASE_URL}/work-histories?${qs}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },
    async get(id) {
        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },
    async create(payload) {
        const res = await fetch(`${API_BASE_URL}/work-histories`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include',
        });
        return handleResponse(res);
    },
    async update(id, payload) {
        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include',
        });
        return handleResponse(res);
    },
    async remove(id) {
        const res = await fetch(`${API_BASE_URL}/work-histories/${id}`, {
            method: 'DELETE',
            headers: authHeaders(),
            credentials: 'include',
        });
        if (res.status === 204) return null;
        return handleResponse(res);
    },
};
// ===== WorkSchedule API (matrix) =====
export const workScheduleApi = {
    async getMatrix({ from, to, branchId }) {
        const p = new URLSearchParams({ from, to, branchId });
        const res = await fetch(`${API_BASE_URL}/work-schedules/matrix?${p}`, {
            headers: authHeaders(), credentials: 'include'
        });
        return handleResponse(res);
    },
    async getCell({ branchId, date, shiftType }) {
        const qs = new URLSearchParams({ branchId, date, shiftType });
        const res = await fetch(`${API_BASE_URL}/work-schedules/cell?${qs}`, {
            headers: authHeaders(), credentials: 'include'
        });
        return handleResponse(res); // [{scheduleId, accountId, accountName}]
    },
    async upsertCellMany(payload) {
        const res = await fetch(`${API_BASE_URL}/work-schedules/upsert-cell-many`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include'
        });
        return handleResponse(res); // 200 (obj) hoặc 204 -> ''
    },
};

// ===== Staff API =====
export const staffApi = {
    async list(branchId) {
        if (!branchId) throw new Error('branchId is required'); // chặn luôn
        const res = await fetch(`${API_BASE_URL}/staffs?branchId=${encodeURIComponent(branchId)}`, {
            credentials: 'include'
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json(); // [{id, fullName}]
    },
};

// ===== Showtime API =====
export const showtimeApi = {
    async search(params) {
        // params: { periodId?, auditoriumId?, from?, to?, page?, size?, sort? }
        const qs = new URLSearchParams(
            Object.entries(params).reduce((acc, [k,v])=>{
                if(v !== null && v !== undefined && v !== '') acc[k]=v;
                return acc;
            }, {})
        ).toString();

        const res = await fetch(`${API_BASE_URL}/showtimes?${qs}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        // Spring Page<ShowtimeResponse> trả về JSON kiểu {content, totalElements, totalPages, number, size, ...}
        return handleResponse(res);
    },

    async get(id) {
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async create(payload){
        const res = await fetch(`${API_BASE_URL}/showtimes`, {
            method: 'POST',
            headers: authHeaders(),
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        return handleResponse(res);
    },

    async update(id, payload) {
        // payload = ShowtimeUpdateRequest
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async remove(id) {
        const res = await fetch(`${API_BASE_URL}/showtimes/${id}`, {
            method: 'DELETE',
            headers: authHeaders(),
            credentials: 'include',
        });
        if (res.status === 204) return null;
        return handleResponse(res);
    },

    async week({ anchor, branchId } = {}) {
        const p = new URLSearchParams();
        if (anchor) p.append('anchor', anchor);       // 'YYYY-MM-DD'
        if (branchId) p.append('branchId', branchId);
        const res = await fetch(`${API_BASE_URL}/showtimes/week?${p.toString()}`, {
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },
};
// Lịch tuần kế (nhóm theo ngày -> phim -> slots)
showtimeApi.nextWeek = async (branchId) => {
    const qs = branchId ? `?branchId=${encodeURIComponent(branchId)}` : '';
    const res = await fetch(`${API_BASE_URL}/showtimes/next-week${qs}`, {
        headers: authHeaders(), credentials: 'include'
    });
    return handleResponse(res); // [{date, movies:[{movieId, movieTitle, posterUrl, slots:[...] }]}]
};

// Ràng buộc tạo/sửa: BE đã chặn; FE có thể kiểm trước (tuỳ).
showtimeApi.isWithinNextWeek = (iso) => {
    const now = new Date();
    const day = now.getDay(); // 0 Sun .. 6 Sat
    const mondayOffset = ((8 - day) % 7) || 7; // tới thứ Hai tuần sau
    const nextMonday = new Date(now.getFullYear(), now.getMonth(), now.getDate() + mondayOffset);
    nextMonday.setHours(0,0,0,0);
    const nextMondayPlus7 = new Date(+nextMonday + 7*86400000);
    const t = new Date(iso);
    return t >= nextMonday && t < nextMondayPlus7;
};
// ===== ScreeningPeriod API =====
export const screeningPeriodApi = {
    async get(id){
        const res = await fetch(`${API_BASE_URL}/screening-periods/${encodeURIComponent(id)}`, {
            headers: authHeaders(),
            credentials: 'include'
        });
        return handleResponse(res);
    },
    async active({ branchId, onDate } = {}) {
        const p = new URLSearchParams();
        if (branchId != null) p.append('branchId', branchId);
        if (onDate) p.append('onDate', onDate); // 'YYYY-MM-DD'
        const res = await fetch(`${API_BASE_URL}/screening-periods/active?${p.toString()}`, {
            headers: authHeaders(),
            credentials: 'include'
        });
        // [{periodId, movieId, movieTitle, branchId, startDate, endDate, duration}]
        return handleResponse(res);
    }
};

// ===== Auditorium API ===== (khớp controller @GetMapping với param branchId)
export const auditoriumApi = {
    async listByBranch(branchId){
        const params = new URLSearchParams({ branchId });
        const res = await fetch(`${API_BASE_URL}/auditoriums?${params}`, {
            headers: authHeaders(),
            credentials: 'include'
        });
        return handleResponse(res);
    }
};
// ===== Membership Level API =====
export const membershipLevelApi = {
    async search(params = {}) {
        const qs = new URLSearchParams();
        if (params.page != null) qs.append("page", params.page);
        if (params.size != null) qs.append("size", params.size);
        if (params.sort) qs.append("sort", params.sort);
        if (params.keyword) qs.append("keyword", params.keyword);

        const res = await fetch(`${API_BASE_URL}/membership-levels?${qs.toString()}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async get(id) {
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            method: 'GET',
            headers: authHeaders(),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async create(payload) {
        const res = await fetch(`${API_BASE_URL}/membership-levels`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async update(id, payload) {
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify(payload),
            credentials: 'include',
        });
        return handleResponse(res);
    },

    async remove(id) {
        const res = await fetch(`${API_BASE_URL}/membership-levels/${id}`, {
            method: 'DELETE',
            headers: authHeaders(),
            credentials: 'include',
        });
        if (res.status === 204) return null;
        return handleResponse(res);
    },
};  // <--- chú ý: ngoặc đóng này là của membershipLevelApi


