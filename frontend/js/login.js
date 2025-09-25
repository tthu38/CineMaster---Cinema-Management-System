import { api } from "../js/api.js"; // import object api (gồm login, register, logout...)

// ====== Khi load trang login ======
window.addEventListener("DOMContentLoaded", () => {
    // Điền số điện thoại nếu đã nhớ
    const remembered = localStorage.getItem("rememberedUsername");
    if (remembered) {
        document.getElementById("username").value = remembered;
        document.getElementById("rememberMe").checked = true;
    }

    // Xóa token cũ để tránh lỗi khi còn session cũ
    localStorage.removeItem("accessToken");
});

// ====== Đăng nhập thường ======
document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const phoneNumber = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const rememberMe = document.getElementById("rememberMe").checked;
    const errorDiv = document.getElementById("error-message");

    // Reset lỗi
    errorDiv.textContent = "";
    errorDiv.classList.add("d-none");

    try {
        // Gọi API login thường
        const data = await api.login({ phoneNumber, password });
        console.log("Login thường response:", data);

        const token = data.accessToken || data.result?.accessToken;
        if (!token) {
            throw new Error("Không nhận được access token từ server");
        }

        // Ghi nhớ số điện thoại nếu chọn
        if (rememberMe) {
            localStorage.setItem("rememberedUsername", phoneNumber);
        } else {
            localStorage.removeItem("rememberedUsername");
        }

        // Lưu token
        localStorage.setItem("accessToken", token);

        // Điều hướng
        window.location.href = "../user/profile.html";
    } catch (err) {
        console.error("Login thường lỗi:", err);
        errorDiv.textContent = err.message || "Sai số điện thoại hoặc mật khẩu";
        errorDiv.classList.remove("d-none");
    }
});

// ====== Google Identity Callback ======
window.handleCredentialResponse = async function (response) {
    try {
        if (!response || !response.credential) {
            console.error("Google Identity: không nhận được credential");
            alert("Không thể đăng nhập bằng Google, vui lòng thử lại!");
            return;
        }

        const idToken = response.credential; // token từ Google
        console.log("Google credential:", idToken.substring(0, 20) + "...");

        // Gọi BE login Google
        const data = await api.googleLogin(idToken);
        console.log("Google login response:", data);

        const token = data.accessToken || data.result?.accessToken;
        if (!token) {
            throw new Error("Không nhận được access token từ server");
        }

        localStorage.setItem("accessToken", token);

        // Nếu BE trả về email → lưu để revoke khi logout
        if (data.email) {
            localStorage.setItem("googleEmail", data.email);
        }

        window.location.href = "../user/profile.html";
    } catch (err) {
        console.error("Google login lỗi:", err);
        alert("Google login thất bại, vui lòng thử lại!");
    }
};
