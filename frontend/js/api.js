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
//=========== Branch =================
const _branchApi = {

    // 📌 Lấy tất cả chi nhánh (Admin/Manager)
    async getAllBranches() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy tất cả chi nhánh đang hoạt động (Client/Staff)
    async getAllActive() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/active`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy danh sách tên chi nhánh (Dropdown)
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/names`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy chi nhánh theo ID (Client/Staff)
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy chi nhánh theo ID (Admin)
    async getByIdAdmin(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/${id}/admin`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo mới chi nhánh
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches`, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật chi nhánh
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: 'PUT',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // DELETE (soft delete)
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // RESTORE (PUT /{id}/restore)
    async restore(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/branches/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};

//================ Combo ==============
const _comboApi = {
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/combos`, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return handleResponse(res);
    },
    // CREATE (multipart/form-data)
    async create(comboData, imageFile) {
        const token = getValidToken();
        if (!token) return null;

        const formData = new FormData();
        formData.append(
            "data", // ✅ trùng @RequestPart("data")
            new Blob([JSON.stringify(comboData)], { type: "application/json" })
        );

        if (imageFile) {
            formData.append("imageFile", imageFile);
        }

        const res = await fetch(`${API_BASE_URL}/combos`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                // ❌ KHÔNG set Content-Type — fetch tự động gắn boundary
            },
            body: formData,
        });

        return handleResponse(res);
    },
    async getById(id) {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    async update(id, comboData, imageFile) {
        const token = getValidToken();
        const formData = new FormData();

        formData.append("data", new Blob([JSON.stringify(comboData)], { type: "application/json" }));
        if (imageFile) {
            formData.append("imageFile", imageFile);
        }

        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
            body: formData,
        });

        return handleResponse(res);
    },

    // DELETE (soft delete)
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/combos/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },

    // RESTORE (PUT /{id}/restore)
    async restore(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/combos/${id}/restore`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};

// ============ Auditorium =================
const _auditoriumApi = {

    // 📌 Lấy tất cả phòng chiếu (Admin / Manager)
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy tất cả phòng chiếu đang hoạt động (Client / Staff)
    async getAllActive() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/active`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy danh sách phòng chiếu theo BranchID (Admin)
    async getByBranch(branchId) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/branch/${branchId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy danh sách phòng chiếu đang hoạt động theo BranchID (Client)
    async getActiveByBranch(branchId) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/branch/${branchId}/active`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy phòng chiếu theo ID (Client / Staff)
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy phòng chiếu theo ID (Admin)
    async getByIdAdmin(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}/admin`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo mới phòng chiếu
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật phòng chiếu
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa mềm (Deactivate)
    async deactivate(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    // 📌 Khôi phục (Activate)
    async activate(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/auditoriums/${id}/activate`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },
};

// ============ SEAT API =================
const _seatApi = {

    // 📌 Lấy tất cả ghế (Admin / Manager)
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy ghế theo ID
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo ghế mới
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật ghế
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa ghế
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/${id}`, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return handleResponse(res);
    },

    // 📌 Tạo hàng loạt ghế (Bulk Create)
    async createBulk(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/bulk`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật hàng loạt ghế (Bulk Update Row)
    async bulkUpdateRow(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/seats/bulk-update-row`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },
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

// ============ SCREENING PERIOD API =================
const _screeningPeriodApi = {
    // 📌 Tạo mới kỳ chiếu
    async create(data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Lấy toàn bộ danh sách kỳ chiếu
    async getAll() {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy kỳ chiếu theo ID
    async getById(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy kỳ chiếu theo BranchID
    async getByBranch(branchId) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods/branch/${branchId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Cập nhật kỳ chiếu
    async update(id, data) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });
        return handleResponse(res);
    },

    // 📌 Xóa kỳ chiếu
    async delete(id) {
        const token = getValidToken();
        if (!token) return null;

        const res = await fetch(`${API_BASE_URL}/screening-periods/${id}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        });
        return handleResponse(res);
    },
};

// ============ MOVIE API =================
const _movieApi = {
    // 📌 Lấy toàn bộ danh sách phim
    async getAll() {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies`, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy phim đang chiếu
    async getNowShowing() {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies/now-showing`, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
            },
        });
        return handleResponse(res);
    },

    // 📌 Lấy phim sắp chiếu
    async getComingSoon() {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/movies/coming-soon`, {
            method: "GET",
            headers: {
                Authorization: token ? `Bearer ${token}` : undefined,
                "Content-Type": "application/json",
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
export const branchApi = _branchApi;
export const comboApi = _comboApi;
export const auditoriumApi = _auditoriumApi;
export const seatApi = _seatApi;
export const seatTypeApi = _seatTypeApi;
export const screeningPeriodApi = _screeningPeriodApi;
export const movieApi = _movieApi;


export { API_BASE_URL };

