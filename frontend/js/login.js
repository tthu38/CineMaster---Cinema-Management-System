// import đúng file api.js hiện có
import { authApi } from "./api.js"; // vì file login.js nằm trong folder "home"

// ========== Khi trang login load ==========
document.addEventListener("DOMContentLoaded", () => {
    const remembered = localStorage.getItem("rememberedUsername");
    if (remembered) {
        document.getElementById("username").value = remembered;
        document.getElementById("rememberMe").checked = true;
    }
    // Xóa token cũ để tránh session cũ
    localStorage.removeItem("accessToken");
});

// ========== Đăng nhập thường ==========
document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const phoneNumber = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const rememberMe = document.getElementById("rememberMe").checked;
    const errorDiv = document.getElementById("error-message");

    try {
        const data = await authApi.login({ phoneNumber, password });
        console.log("✅ Login response:", data);

        const token = data.accessToken || data.result?.accessToken;
        if (!token) throw new Error("Không nhận được access token từ server");

        // Lưu token trước để dùng cho API profile
        localStorage.setItem("accessToken", token);

        // ✅ Gọi API profile để lấy đủ thông tin user
        const res = await fetch("http://localhost:8080/api/v1/users/profile", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Không lấy được thông tin hồ sơ người dùng");
        const profileData = await res.json();
        const profile = profileData.result;

        // ✅ Lưu toàn bộ thông tin user vào localStorage
        localStorage.setItem("accountId", profile.id);
        localStorage.setItem("fullName", profile.fullName || "");
        localStorage.setItem("email", profile.email || "");
        localStorage.setItem("avatarUrl", profile.avatarUrl || "../assets/default-avatar.png");
        localStorage.setItem("role", profile.roleName || "Customer");
        localStorage.setItem("branchId", profile.branchId || "");
        localStorage.setItem("branchName", profile.branchName || "");
        localStorage.setItem("loyaltyPoints", profile.loyaltyPoints || 0);

        // ✅ Ghi nhớ tài khoản nếu chọn “Remember me”
        if (rememberMe) localStorage.setItem("rememberedUsername", phoneNumber);
        else localStorage.removeItem("rememberedUsername");

        // ✅ Chuyển hướng theo vai trò
        redirectByRole(profile.roleName || "Customer");

    } catch (err) {
        console.error("❌ Login lỗi:", err);
        errorDiv.textContent = err.message || "Sai số điện thoại hoặc mật khẩu";
        errorDiv.classList.remove("d-none");
    }
});

// ========== Google Login ==========
window.handleCredentialResponse = async function (response) {
    try {
        const idToken = response?.credential;
        if (!idToken) throw new Error("Không có credential từ Google");

        const data = await authApi.googleLogin(idToken);
        console.log("✅ Google login response:", data);

        const token = data.accessToken || data.result?.accessToken;
        if (!token) throw new Error("Không nhận được access token từ server");

        // Lưu token trước để gọi API profile
        localStorage.setItem("accessToken", token);

        // ✅ Lấy thông tin user từ profile API
        const res = await fetch("http://localhost:8080/api/v1/users/profile", {
            headers: { "Authorization": "Bearer " + token }
        });

        const profileData = await res.json();
        const profile = profileData.result;

        localStorage.setItem("accountId", profile.id);
        localStorage.setItem("fullName", profile.fullName || "");
        localStorage.setItem("email", profile.email || "");
        localStorage.setItem("avatarUrl", profile.avatarUrl || "../assets/default-avatar.png");
        localStorage.setItem("role", profile.roleName || "Customer");

        redirectByRole(profile.roleName || "Customer");

    } catch (err) {
        console.error("❌ Google login lỗi:", err);
        alert(err.message || "Google login thất bại!");
    }
};

// ========== Hàm chuyển hướng theo role ==========
function redirectByRole(role) {
    console.log("➡️ Redirect by role:", role);
    switch (role) {
        case "Admin":
            window.location.href = "../home/dashboardAdmin.html";
            break;
        case "Manager":
            window.location.href = "../home/dashboardManager.html";
            break;
        case "Staff":
            window.location.href = "../home/dashboardStaff.html";
            break;
        default:
            window.location.href = "../home/home.html";
            break;
    }
}
