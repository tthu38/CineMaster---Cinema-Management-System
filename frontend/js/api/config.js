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

// ==================== RESPONSE HANDLER ====================
export async function handleResponse(res) {
    const ct = res.headers.get('content-type') || '';
    let data;

    if (ct.includes('application/json')) {
        data = await res.json().catch(() => ({}));
    } else {
        data = await res.text().catch(() => '');
    }

    if (!res.ok) {
        // Lấy thông điệp từ body nếu có
        const msg = typeof data === 'string' ? data : data.message || '';

        let friendlyMsg;
        switch (res.status) {
            case 400:
                friendlyMsg = msg || "Yêu cầu không hợp lệ.";
                break;
            case 401:
                friendlyMsg = "Bạn cần đăng nhập để thực hiện thao tác này.";
                break;
            case 403:
                friendlyMsg = "Bạn không có quyền truy cập chức năng này.";
                break;
            case 404:
                friendlyMsg = "Không tìm thấy dữ liệu hoặc tài nguyên.";
                break;
            case 500:
                friendlyMsg = "Không quyền truy cập chức năng này.";
                break;
            default:
                friendlyMsg = msg || `Lỗi không xác định (HTTP ${res.status})`;
        }

        // Ném lỗi để hiển thị trong popup SweetAlert
        throw new Error(`HTTP ${res.status}: ${friendlyMsg}`);
    }

    // 🧩 Nếu backend trả { code, message, result } → trả result
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
