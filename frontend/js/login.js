import { apiPost } from "../js/api.js";

// ====== Tự điền số điện thoại nếu có nhớ ======
window.addEventListener("DOMContentLoaded", () => {
    const remembered = localStorage.getItem("rememberedUsername");
    if (remembered) {
        document.getElementById("username").value = remembered;
        document.getElementById("rememberMe").checked = true;
    }
});

// ====== Đăng nhập bằng tài khoản thường ======
document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const phoneNumber = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const rememberMe = document.getElementById("rememberMe").checked;

    try {
        const data = await apiPost("/api/auth/login", { phoneNumber, password });

        console.log("Login thường thành công:", data);

        if (rememberMe) {
            localStorage.setItem("rememberedUsername", phoneNumber);
        } else {
            localStorage.removeItem("rememberedUsername");
        }

        localStorage.setItem("accessToken", data.accessToken);

        window.location.href = "../home/index.html";
    } catch (err) {
        console.error("Login thường thất bại:", err);
        const errorDiv = document.getElementById("error-message");
        errorDiv.textContent = err.message || "Sai số điện thoại hoặc mật khẩu";
        errorDiv.classList.remove("d-none");
    }
});

// ====== Callback Google Identity ======
window.handleCredentialResponse = async function (response) {
    try {
        if (!response || !response.credential) {
            console.error("Google Identity: không nhận được credential");
            alert("Không thể đăng nhập bằng Google, vui lòng thử lại!");
            return;
        }

        const idToken = response.credential; // token từ Google
        console.log("Google credential nhận được:", idToken.substring(0, 20) + "...");

        const data = await apiPost("/api/auth/google", { token: idToken }, false);

        console.log("Google login thành công:", data);

        localStorage.setItem("accessToken", data.accessToken);

        // Nếu backend trả về email trong payload, lưu lại để revoke khi logout
        if (data.email) {
            localStorage.setItem("googleEmail", data.email);
        }

        window.location.href = "../home/index.html";
    } catch (err) {
        console.error("Google login thất bại:", err);
        alert("Google login thất bại, vui lòng thử lại!");
    }
};
