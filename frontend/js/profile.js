import { api } from "../js/api.js"; // api.js phải export { api }

function getToken() {
    return localStorage.getItem("accessToken");
}

// ===== Render UI với fallback =====
function renderProfile(p) {
    document.getElementById("fullNameDisplay").textContent = `Welcome, ${
        p.fullName && p.fullName.trim() !== "" ? p.fullName : "User"
    }!`;

    // Avatar: fallback nếu null hoặc trống
    document.getElementById("avatarImg").src =
        p.avatarUrl && p.avatarUrl.trim() !== ""
            ? p.avatarUrl
            : "/image/avatar.png";

    document.getElementById("infoFullName").textContent =
        p.fullName && p.fullName.trim() !== "" ? p.fullName : "Chưa cập nhật";
    document.getElementById("infoEmail").textContent =
        p.email && p.email.trim() !== "" ? p.email : "Chưa cập nhật";
    document.getElementById("infoPhone").textContent =
        p.phoneNumber && p.phoneNumber.trim() !== ""
            ? p.phoneNumber
            : "Chưa cập nhật";
    document.getElementById("infoAddress").textContent =
        p.address && p.address.trim() !== "" ? p.address : "Chưa cập nhật";
    document.getElementById("infoRole").textContent = p.roleName ?? "User";
    document.getElementById("infoCreatedAt").textContent = p.createdAt
        ? new Date(p.createdAt).toLocaleDateString("vi-VN")
        : "Chưa cập nhật";
    document.getElementById("infoPoints").textContent = p.loyaltyPoints ?? 0;

    // Form Edit (không set email vì đổi email qua OTP)
    document.getElementById("fullNameInput").value = p.fullName ?? "";
    document.getElementById("phoneInput").value = p.phoneNumber ?? "";
    document.getElementById("addressInput").value = p.address ?? "";
}

// ===== Load profile =====
async function loadProfile() {
    const token = getToken();
    if (!token) {
        window.location.href = "login.html";
        return;
    }

    try {
        const data = await api.getProfile();
        console.log("Profile data:", data);
        renderProfile(data);
    } catch (err) {
        console.error("Error loading profile:", err);
        alert(err.message || "Không tải được thông tin user, vui lòng đăng nhập lại");
        localStorage.removeItem("accessToken");
        window.location.href = "login.html";
    }
}

// ===== Tabs =====
function initTabs() {
    const tabs = document.querySelectorAll("#profileTabs .nav-link");
    tabs.forEach((tab) => {
        tab.addEventListener("click", function () {
            tabs.forEach((t) => t.classList.remove("active"));
            this.classList.add("active");

            document
                .querySelectorAll(".tab-content > div")
                .forEach((c) => (c.style.display = "none"));
            const tabId = this.getAttribute("data-tab");
            document.getElementById(tabId).style.display = "block";
        });
    });
}

// ===== Logout =====
function initLogout() {
    document.getElementById("logoutBtn").addEventListener("click", () => {
        api.logout();
    });
}

// ===== Edit profile =====
function initEditProfile() {
    const form = document.getElementById("editForm");
    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const userData = {};
        const fullName = document.getElementById("fullNameInput").value.trim();
        const phone = document.getElementById("phoneInput").value.trim();
        const address = document.getElementById("addressInput").value.trim();
        const avatarFile = document.getElementById("avatarFile").files[0];
        if (avatarFile) {
            const newAvatarUrl = await api.uploadAvatar(avatarFile);
            document.getElementById("avatarImg").src = newAvatarUrl;
        }


        if (fullName) userData.fullName = fullName;
        if (phone) userData.phoneNumber = phone;
        if (address) userData.address = address;

        try {
            // Nếu có ảnh → upload avatar trước
            if (avatarFile) {
                const avatarUrl = await api.uploadAvatar(avatarFile);
                document.getElementById("avatarImg").src = avatarUrl; // cập nhật ngay UI
            }

            // Nếu có dữ liệu profile → gửi update
            if (Object.keys(userData).length > 0) {
                await api.updateProfile(userData);
            }

            alert("Cập nhật thành công!");
            loadProfile();
            form.reset();
        } catch (err) {
            console.error("Update profile error:", err);
            alert(err.message || "Lỗi khi cập nhật thông tin");
        }
    });
}

// ===== Change password =====
function initChangePassword() {
    const form = document.getElementById("passwordForm");
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const currentPassword = document
            .getElementById("currentPassword")
            .value.trim();
        const newPassword = document.getElementById("newPassword").value.trim();
        const confirmPassword = document
            .getElementById("confirmPassword")
            .value.trim();

        if (newPassword !== confirmPassword) {
            alert("Mật khẩu mới không khớp");
            return;
        }

        try {
            await api.changePassword({ currentPassword, newPassword });
            alert("Đổi mật khẩu thành công!");
            form.reset();
        } catch (err) {
            console.error("Change password error:", err);
            alert(err.message || "Lỗi khi đổi mật khẩu");
        }
    });
}

// ===== Change Email with OTP =====
function initChangeEmail() {
    const form = document.getElementById("changeEmailForm");
    const sendOtpBtn = document.getElementById("sendOtpBtn");
    const otpSection = document.getElementById("otpSection");

    if (!form || !sendOtpBtn) return; // fallback nếu chưa có UI

    // Gửi OTP
    sendOtpBtn.addEventListener("click", async () => {
        const newEmail = document.getElementById("newEmail").value.trim();
        if (!newEmail) {
            alert("Vui lòng nhập email mới");
            return;
        }
        try {
            await api.sendOtpChangeEmail(newEmail);
            alert("OTP đã được gửi đến email mới!");
            otpSection.style.display = "block";
        } catch (err) {
            console.error("Send OTP error:", err);
            alert(err.message || "Không gửi được OTP");
        }
    });

    // Xác thực OTP và đổi email
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const newEmail = document.getElementById("newEmail").value.trim();
        const otp = document.getElementById("otpCode").value.trim();
        if (!otp) {
            alert("Vui lòng nhập OTP");
            return;
        }

        try {
            await api.verifyEmailChange(newEmail, otp);
            alert("Đổi email thành công!");
            loadProfile();
            form.reset();
            otpSection.style.display = "none";
        } catch (err) {
            console.error("Verify email error:", err);
            alert(err.message || "Xác thực OTP thất bại");
        }
    });
}

// ===== Init =====
document.addEventListener("DOMContentLoaded", () => {
    loadProfile();
    initTabs();
    initLogout();
    initEditProfile();
    initChangePassword();
    initChangeEmail();
});
