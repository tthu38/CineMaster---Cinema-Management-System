import { apiPost } from "../js/api.js"; // api.js cùng thư mục js/

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

        console.log("Login thành công:", data);

        // Lưu số điện thoại nếu chọn "ghi nhớ"
        if (rememberMe) {
            localStorage.setItem("rememberedUsername", phoneNumber);
        } else {
            localStorage.removeItem("rememberedUsername");
        }

        // Lưu token để gọi API protected sau này
        localStorage.setItem("accessToken", data.accessToken);

        // ✅ Redirect sang trang index.html (trong folder /home/)
        window.location.href = "../home/index.html";
    } catch (err) {
        const errorDiv = document.getElementById("error-message");
        errorDiv.textContent = err.message || "Sai số điện thoại hoặc mật khẩu";
        errorDiv.classList.remove("d-none");
    }
});

// ====== Đăng nhập bằng Google ======
document.getElementById("googleLoginBtn").addEventListener("click", () => {
    // Gọi đúng endpoint OAuth2 login của backend (có context-path /demo)
    window.location.href = "http://localhost:8080/demo/oauth2/authorization/google";
});
