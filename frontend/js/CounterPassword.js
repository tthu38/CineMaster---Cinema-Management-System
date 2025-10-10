import { api } from "../js/api.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("passwordForm");
    const messageBox = document.getElementById("messageBox");

    function showMessage(msg, type = "success") {
        messageBox.className = `alert alert-${type}`;
        messageBox.textContent = msg;
        messageBox.classList.remove("d-none");
    }

    // Lấy token từ URL
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");

    if (!token) {
        showMessage("❌ Liên kết không hợp lệ hoặc thiếu token", "danger");
        form.classList.add("d-none");
        return;
    }

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const newPassword = document.getElementById("newPassword").value.trim();
        const confirmPassword = document.getElementById("confirmPassword").value.trim();

        if (newPassword.length < 6) {
            showMessage("⚠️ Mật khẩu phải từ 6 ký tự trở lên", "danger");
            return;
        }

        if (newPassword !== confirmPassword) {
            showMessage("⚠️ Mật khẩu xác nhận không khớp", "danger");
            return;
        }

        try {
            await api.resetByToken(token, newPassword);
            showMessage("🎉 Đặt mật khẩu thành công! Bạn sẽ được chuyển về trang chính.", "success");
            setTimeout(() => (window.location.href = "../home/home.html"), 2000);
        } catch (err) {
            console.error(err);
            showMessage("❌ Liên kết hết hạn hoặc không hợp lệ", "danger");
        }

    });
});
