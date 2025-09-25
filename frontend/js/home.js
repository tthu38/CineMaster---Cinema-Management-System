// ====== Cấu hình cơ bản ======
const LOGIN_PAGE = "../user/login.html";
const TOKEN_KEY = "accessToken";
const HOME_PAGE = "home.html";

// ====== Logout ======
window.handleLogout = async function handleLogout() {
    try {
        const token = localStorage.getItem("accessToken");
        if (token) {
            await fetch("http://localhost:8080/api/v1/auth/logout", {
                method: "POST",
                headers: { "Authorization": `Bearer ${token}` }
            });
        }
    } catch (e) {
        console.error("Logout error:", e);
    } finally {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userInfo");
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
