// ==================== CONFIG BASE ====================
export const API_BASE_URL = 'http://localhost:8080/api/v1';

// ==================== TOKEN HANDLER ====================
export function validateToken(token) {
    if (!token) return false;
    try {
        const tokenData = JSON.parse(atob(token.split('.')[1]));
        const isValid = tokenData.exp * 1000 > Date.now();
        if (!isValid) localStorage.removeItem('accessToken');
        return isValid;
    } catch (err) {
        console.error('Token validation error:', err);
        localStorage.removeItem('accessToken');
        return false;
    }
}

export function getValidToken() {
    const token = localStorage.getItem('accessToken');
    if (!token || !validateToken(token)) return null;
    return token;
}

// ==================== RESPONSE HANDLER (CLEAN VERSION) ====================
export async function handleResponse(res) {
    const ct = res.headers.get('content-type') || '';
    let data;

    // Parse response body
    if (ct.includes('application/json')) {
        data = await res.json().catch(() => ({}));
    } else {
        data = await res.text().catch(() => '');
    }

    if (!res.ok) {
        const msg =
            typeof data === 'string'
                ? data
                : data.message || data.error || '';

        const error = new Error(msg || `HTTP ${res.status} error`);
        error.status = res.status;
        error.raw = data;
        throw error; // 👉 chỉ throw — không alert
    }

    return data.result ?? data;
}



// ==================== AUTH CHECK ====================
export function requireAuth() {
    const token = getValidToken();
    if (!token) {
        window.location.href = "../user/login.html";
        return null;
    }
    return token;
}

export const authGet = async (url) => {
    const token = localStorage.getItem("accessToken");
    const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error("Lỗi khi fetch dữ liệu");
    return res.json();
};

