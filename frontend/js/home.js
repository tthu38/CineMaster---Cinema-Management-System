// ====== Cấu hình cơ bản ======
const TOKEN_KEY = "accessToken";
const HOME_PAGE = "home.html";

// ====== Logout ======
window.handleLogout = function handleLogout() {
    try {
        localStorage.removeItem(TOKEN_KEY);
        window.location.replace(HOME_PAGE);
    } catch (e) {
        window.location.href = HOME_PAGE;
    }
};

// ====== Ẩn/hiện nút Logout theo trạng thái đăng nhập ======
document.addEventListener("DOMContentLoaded", () => {
    // 1) Lấy token từ query (nếu có - Google login redirect về)
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
        // Xóa token khỏi URL để không lộ
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // 2) Kiểm tra trạng thái login
    const isLoggedIn = !!localStorage.getItem(TOKEN_KEY);
    const btn = document.querySelector(".logout-btn");
    if (btn) {
        btn.classList.toggle("d-none", !isLoggedIn);
    }
});
