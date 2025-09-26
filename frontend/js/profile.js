// frontend/js/profile.js
import { api } from "./api.js";

function getToken() {
    return localStorage.getItem("accessToken");
}

/* ------- Render UI ------- */
function renderProfile(p) {
    const safe = (v, fallback = "Chưa cập nhật") =>
        v && String(v).trim() !== "" ? v : fallback;

    // Header chào mừng (trên card)
    const fullName = safe(p.fullName, "User");
    const headerName = document.getElementById("fullNameDisplay");
    if (headerName) headerName.textContent = `Welcome, ${fullName}!`;

    // Avatar
    const avatar = document.getElementById("avatarImg");
    if (avatar) {
        const src =
            p.avatarUrl && p.avatarUrl.trim() !== "" ? p.avatarUrl : "/image/avatar.png";
        avatar.src = src.startsWith("/") || src.startsWith("http") ? src : "/" + src;
    }

    // Thông tin
    const set = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };
    set("infoFullName", fullName);
    set("infoEmail", safe(p.email));
    set("infoPhone", safe(p.phoneNumber));
    set("infoAddress", safe(p.address));
    set("infoRole", p.roleName ?? "Customer");
    set(
        "infoCreatedAt",
        p.createdAt ? new Date(p.createdAt).toLocaleDateString("vi-VN") : "Chưa cập nhật"
    );
    const pointsEl = document.getElementById("infoPoints");
    if (pointsEl) pointsEl.textContent = p.loyaltyPoints ?? 0;

    // Form edit
    const fullNameInput = document.getElementById("fullNameInput");
    const phoneInput = document.getElementById("phoneInput");
    const addressInput = document.getElementById("addressInput");
    if (fullNameInput) fullNameInput.value = p.fullName ?? "";
    if (phoneInput) phoneInput.value = p.phoneNumber ?? "";
    if (addressInput) addressInput.value = p.address ?? "";

    // Đồng bộ tên ở customer-header nếu có
    const headerFullName = document.getElementById("cmFullName");
    if (headerFullName) headerFullName.textContent = fullName;
}

/* ------- Load profile ------- */
async function loadProfile() {
    const token = getToken();
    if (!token) {
        // nếu chưa login → về trang login
        location.href = "../user/login.html";
        return;
    }
    try {
        const data = await api.getProfile();
        renderProfile(data);
    } catch (err) {
        console.error("Error loading profile:", err);
        alert(err.message || "Không tải được thông tin user, vui lòng đăng nhập lại");
        localStorage.removeItem("accessToken");
        location.href = "../user/login.html";
    }
}

/* ------- Tabs ------- */
function initTabs() {
    const tabs = document.querySelectorAll("#profileTabs .nav-link");
    const panes = {
        info: document.getElementById("info"),
        edit: document.getElementById("edit"),
        password: document.getElementById("password"),
    };
    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");
            const selected = tab.getAttribute("data-tab");
            Object.entries(panes).forEach(([k, el]) => {
                if (!el) return;
                el.style.display = k === selected ? "block" : "none";
                if (k === selected) el.classList.add("active");
                else el.classList.remove("active");
            });
        });
    });
}

/* ------- Logout (dự phòng nếu header chưa gắn) ------- */
function initLogout() {
    const btn = document.getElementById("logoutBtn");
    if (!btn) return;
    btn.addEventListener("click", () => {
        api.logout?.(); // nếu bạn có api.logout
        localStorage.removeItem("accessToken");
        location.href = "../user/login.html";
    });
}

/* ------- Edit profile ------- */
function initEditProfile() {
    const form = document.getElementById("editForm");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        // Thu thập dữ liệu
        const userData = {};
        const fullName = document.getElementById("fullNameInput")?.value.trim();
        const phone = document.getElementById("phoneInput")?.value.trim();
        const address = document.getElementById("addressInput")?.value.trim();

        if (fullName) userData.fullName = fullName;
        if (phone) userData.phoneNumber = phone;
        if (address) userData.address = address;

        // avatarFile có thể KHÔNG tồn tại ở giao diện mới → phải check cẩn thận
        const avatarInput = document.getElementById("avatarFile");
        const avatarFile = avatarInput?.files?.[0] || null;

        try {
            // Nếu có chọn ảnh trong form (tuỳ bạn có để input này hay không)
            if (avatarFile) {
                const newUrl = await api.uploadAvatar(avatarFile);
                const img = document.getElementById("avatarImg");
                if (img) img.src = (newUrl.startsWith("http") ? newUrl : newUrl) + `?t=${Date.now()}`;
            }

            if (Object.keys(userData).length === 0 && !avatarFile) {
                alert("Bạn chưa thay đổi thông tin nào.");
                return;
            }

            // Cập nhật thông tin
            if (Object.keys(userData).length > 0) {
                await api.updateProfile(userData);
            }

            alert("Cập nhật thành công!");
            await loadProfile();
            form.reset();
        } catch (err) {
            console.error("Update profile error:", err);
            alert(err.message || "Lỗi khi cập nhật thông tin");
        }
    });
}

/* ------- Change password ------- */
function initChangePassword() {
    const form = document.getElementById("passwordForm");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const currentPassword = document.getElementById("currentPassword")?.value.trim();
        const newPassword = document.getElementById("newPassword")?.value.trim();
        const confirmPassword = document.getElementById("confirmPassword")?.value.trim();

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

/* ------- Change Email with OTP ------- */
function initChangeEmail() {
    const form = document.getElementById("changeEmailForm");
    const sendOtpBtn = document.getElementById("sendOtpBtn");
    const otpSection = document.getElementById("otpSection");

    if (!form || !sendOtpBtn) return;

    sendOtpBtn.addEventListener("click", async () => {
        const newEmail = document.getElementById("newEmail")?.value.trim();
        if (!newEmail) {
            alert("Vui lòng nhập email mới");
            return;
        }
        try {
            await api.sendOtpChangeEmail(newEmail);
            alert("OTP đã được gửi đến email mới!");
            if (otpSection) otpSection.style.display = "block";
        } catch (err) {
            console.error("Send OTP error:", err);
            alert(err.message || "Không gửi được OTP");
        }
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const newEmail = document.getElementById("newEmail")?.value.trim();
        const otp = document.getElementById("otpCode")?.value.trim();
        if (!otp) {
            alert("Vui lòng nhập OTP");
            return;
        }
        try {
            await api.verifyEmailChange(newEmail, otp);
            alert("Đổi email thành công!");
            if (otpSection) otpSection.style.display = "none";
            form.reset();
            await loadProfile();
        } catch (err) {
            console.error("Verify email error:", err);
            alert(err.message || "Xác thực OTP thất bại");
        }
    });
}

/* ------- Khởi tạo ------- */
document.addEventListener("DOMContentLoaded", () => {
    loadProfile();
    initTabs();
    initLogout();
    initEditProfile();
    initChangePassword();
    initChangeEmail();
});

// Cho script avatar nhanh (module khác) có thể gọi lại
window.loadProfile = loadProfile;
