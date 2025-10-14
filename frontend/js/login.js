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

        // Lấy thông tin từ backend
        const token = data.accessToken || data.result?.accessToken;
        const role = data.role || data.result?.role || "Customer";
        const fullName = data.fullName || data.result?.fullName || "";
        const email = data.email || data.result?.email || "";
        const branchId = data.branchId || data.result?.branchId || null;
        const branchName = data.branchName || data.result?.branchName || "";


        if (!token) throw new Error("Không nhận được access token từ server");

        // Lưu vào localStorage
        localStorage.setItem("accessToken", token);
        localStorage.setItem("role", role);
        localStorage.setItem("fullName", fullName);
        localStorage.setItem("email", email);
        localStorage.setItem("branchId", branchId);
        localStorage.setItem("branchName", branchName);


        // Ghi nhớ tài khoản nếu chọn “Remember me”
        if (rememberMe) localStorage.setItem("rememberedUsername", phoneNumber);
        else localStorage.removeItem("rememberedUsername");

        // ✅ Chuyển hướng theo role
        redirectByRole(role);

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
        const role = data.role || data.result?.role || "Customer";
        const fullName = data.fullName || data.result?.fullName || "";
        const email = data.email || data.result?.email || "";

        if (!token) throw new Error("Không nhận được access token từ server");

        // Lưu vào localStorage
        localStorage.setItem("accessToken", token);
        localStorage.setItem("role", role);
        localStorage.setItem("fullName", fullName);
        localStorage.setItem("email", email);

        // ✅ Chuyển hướng theo role
        redirectByRole(role);

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
