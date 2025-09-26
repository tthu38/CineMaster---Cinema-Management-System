import { api } from "../js/api.js";

document.addEventListener("DOMContentLoaded", () => {
    // Lấy các phần tử
    const requestOtpForm = document.getElementById("requestOtpForm");
    const verifyOtpForm = document.getElementById("verifyOtpForm");
    const resetForm = document.getElementById("resetForm");
    const messageBox = document.getElementById("messageBox");

    const emailInput = document.getElementById("email");
    const otpCodeHiddenInput = document.getElementById("otpCode");
    const otpInputs = verifyOtpForm.querySelectorAll(".otp-input");

    const newPasswordInput = document.getElementById("newPassword");
    const confirmPasswordInput = document.getElementById("confirmPassword");

    let currentEmail = null;
    let lastOtp = null; // lưu OTP để dùng cho bước đặt lại mật khẩu

    // ===== Helper hiển thị / ẩn thông báo =====
    function showMessage(msg, type = "success") {
        messageBox.classList.remove("d-none", "alert-danger", "alert-success", "message-success-custom");

        if (type === "success") {
            messageBox.classList.add("alert", "alert-success", "message-success-custom");
            messageBox.innerHTML = `<span class="alert-icon">✔</span> ${msg}`;
        } else {
            messageBox.classList.add("alert", "alert-danger");
            messageBox.textContent = msg;
        }

        messageBox.style.display = "flex";
    }

    function hideMessage() {
        messageBox.classList.add("d-none");
        messageBox.classList.remove("alert-success", "alert-danger", "message-success-custom");
        messageBox.innerHTML = "";
    }

    // ===== LOGIC 6 Ô INPUT OTP =====
    otpInputs.forEach((input, index) => {
        input.addEventListener("input", () => {
            if (input.value.length === 1 && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }
            updateFullOtpCode();
        });

        input.addEventListener("keydown", (e) => {
            if (e.key === "Backspace" && input.value.length === 0 && index > 0) {
                otpInputs[index - 1].focus();
            }
        });
    });

    function updateFullOtpCode() {
        let otpValue = "";
        otpInputs.forEach(input => otpValue += input.value);
        otpCodeHiddenInput.value = otpValue;
    }

    // ===== BƯỚC 1: GỬI OTP =====
    requestOtpForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const email = emailInput.value.trim();

        if (!email) {
            showMessage("⚠️ Vui lòng nhập email", "danger");
            return;
        }

        try {
            await api.requestPasswordReset(email);
            currentEmail = email;

            requestOtpForm.classList.add("d-none");
            verifyOtpForm.classList.remove("d-none");

            showMessage(`🔑 Mã xác thực (OTP) đã được gửi về email: ${email}`, "success");

            otpInputs[0].focus();
        } catch (err) {
            console.error("Lỗi gửi OTP:", err);
            showMessage(err.message || "❌ Không gửi được mã xác thực (OTP)", "danger");
        }
    });

    // ===== BƯỚC 2: XÁC THỰC OTP =====
    verifyOtpForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const otp = otpCodeHiddenInput.value.trim();
        if (otp.length !== 6) {
            showMessage("⚠️ Vui lòng nhập đủ 6 số của mã xác thực (OTP)", "danger");
            return;
        }

        lastOtp = otp; // lưu lại OTP

        verifyOtpForm.classList.add("d-none");
        resetForm.classList.remove("d-none");

        showMessage("✅ Xác thực OTP thành công. Vui lòng đặt mật khẩu mới.", "success");
        newPasswordInput.focus();

        otpInputs.forEach(input => input.value = "");
        otpCodeHiddenInput.value = "";
    });

    // ===== BƯỚC 3: ĐẶT MẬT KHẨU MỚI =====
    resetForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const newPassword = newPasswordInput.value.trim();
        const confirmPassword = confirmPasswordInput.value.trim();

        if (newPassword.length < 6) {
            showMessage("⚠️ Mật khẩu phải có ít nhất 6 ký tự", "danger");
            return;
        }

        if (newPassword !== confirmPassword) {
            showMessage("⚠️ Mật khẩu xác nhận không khớp", "danger");
            return;
        }

        try {
            await api.resetPassword(currentEmail, lastOtp, newPassword);
            showMessage("🎉 Đặt lại mật khẩu thành công! Bạn có thể đăng nhập lại.", "success");

            setTimeout(() => {
                window.location.href = "login.html";
            }, 2000);
        } catch (err) {
            console.error("Lỗi đặt lại mật khẩu:", err);
            showMessage(err.message || "❌ Đặt lại mật khẩu thất bại", "danger");
        }
    });
});
