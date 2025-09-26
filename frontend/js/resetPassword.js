import { api } from "../js/api.js";

document.addEventListener("DOMContentLoaded", () => {
    const requestOtpForm = document.getElementById("requestOtpForm");
    const resetForm = document.getElementById("resetForm");
    const messageBox = document.getElementById("messageBox");
    const otpNotice = document.getElementById("otpNotice");

    let currentEmail = null; // lưu email người dùng nhập

    // ===== Bước 1: gửi OTP =====
    requestOtpForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const email = document.getElementById("email").value.trim();

        if (!email) {
            showMessage("Vui lòng nhập email", "danger");
            return;
        }

        try {
            await api.requestPasswordReset(email);
            currentEmail = email;

            // Hiện thông báo OTP đã gửi
            otpNotice.textContent = `✅ OTP đã được gửi về email: ${email}`;
            showMessage("OTP đã được gửi, vui lòng kiểm tra email!", "success");

            // Ẩn form nhập email, hiện form reset
            requestOtpForm.classList.add("d-none");
            resetForm.classList.remove("d-none");
        } catch (err) {
            console.error("Send OTP error:", err);
            showMessage(err.message || "Không gửi được OTP", "danger");
        }
    });

    // ===== Bước 2: xác thực OTP và đặt mật khẩu mới =====
    resetForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const otp = document.getElementById("otpCode").value.trim();
        const newPassword = document.getElementById("newPassword").value.trim();
        const confirmPassword = document.getElementById("confirmPassword").value.trim();

        if (!otp) {
            showMessage("Vui lòng nhập OTP", "danger");
            return;
        }

        if (newPassword.length < 6) {
            showMessage("Mật khẩu phải từ 6 ký tự trở lên", "danger");
            return;
        }

        if (newPassword !== confirmPassword) {
            showMessage("Mật khẩu xác nhận không khớp", "danger");
            return;
        }

        try {
            await api.resetPassword(currentEmail, otp, newPassword);
            showMessage("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập lại.", "success");

            // Sau 2s quay về login
            setTimeout(() => {
                window.location.href = "login.html";
            }, 2000);
        } catch (err) {
            console.error("Reset password error:", err);
            showMessage(err.message || "Đặt lại mật khẩu thất bại", "danger");
        }
    });

    // ===== Helper hiển thị thông báo =====
    function showMessage(msg, type) {
        messageBox.textContent = msg;
        messageBox.className = `alert alert-${type} text-center`;
        messageBox.classList.remove("d-none");
    }
});
