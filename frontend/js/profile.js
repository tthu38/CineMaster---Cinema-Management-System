import { apiGet, apiPost, apiDelete } from "../js/api.js";

// --- Load Profile ---
async function loadProfile() {
    try {
        // Lấy cache trước (nếu có)
        const cached = localStorage.getItem("userInfo");
        if (cached) renderProfile(JSON.parse(cached));

        // Gọi API BE để lấy dữ liệu mới nhất
        const data = await apiGet("/api/auth/profile");
        renderProfile(data);

        // Cập nhật cache
        localStorage.setItem("userInfo", JSON.stringify(data));
    } catch (err) {
        alert(err.message || "Bạn cần đăng nhập lại");
        window.location.href = "login.html";
    }
}

// --- Render UI ---
function renderProfile(data) {
    document.getElementById("fullNameDisplay").textContent = `Welcome, ${data.fullName}!`;
    document.getElementById("avatarImg").src = data.avatarUrl || "/image/avata.png";
    document.getElementById("infoFullName").textContent = data.fullName;
    document.getElementById("infoEmail").textContent = data.email;
    document.getElementById("infoPhone").textContent = data.phoneNumber;
    document.getElementById("infoAddress").textContent = data.address;
    document.getElementById("infoRole").textContent = data.roleName;
    document.getElementById("infoCreatedAt").textContent = data.createdAt;
    document.getElementById("infoPoints").textContent = data.loyaltyPoints;

    // form edit
    document.getElementById("fullNameInput").value = data.fullName;
    document.getElementById("emailInput").value = data.email;
    document.getElementById("phoneInput").value = data.phoneNumber;
    document.getElementById("addressInput").value = data.address;
}

// --- Gọi khi mở trang ---
loadProfile();

// --- Update Profile (có hỗ trợ upload avatar) ---
document.getElementById("editForm").addEventListener("submit", async e => {
    e.preventDefault();
    const formData = new FormData(e.target); // Lấy toàn bộ input trong form

    try {
        const token = localStorage.getItem("accessToken");
        const res = await fetch("http://localhost:8080/api/auth/profile", {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` }, // KHÔNG set Content-Type
            body: formData
        });

        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();

        alert("Cập nhật thành công!");
        renderProfile(data);
        localStorage.setItem("userInfo", JSON.stringify(data));
    } catch (err) {
        alert(err.message || "Cập nhật thất bại");
    }
});

// --- Change Password ---
document.getElementById("passwordForm").addEventListener("submit", async e => {
    e.preventDefault();
    const payload = {
        currentPassword: document.getElementById("currentPassword").value,
        newPassword: document.getElementById("newPassword").value,
        confirmNewPassword: document.getElementById("confirmPassword").value
    };
    try {
        await apiPost("/api/auth/change-password", payload);
        alert("Đổi mật khẩu thành công!");
        e.target.reset();
    } catch (err) {
        alert(err.message || "Đổi mật khẩu thất bại");
    }
});

// --- Delete Profile ---
document.getElementById("deleteBtn")?.addEventListener("click", async () => {
    if (confirm("Bạn có chắc chắn muốn xóa tài khoản không?")) {
        try {
            await apiDelete("/api/auth/profile");
            alert("Tài khoản đã bị xóa!");
            localStorage.removeItem("accessToken");
            localStorage.removeItem("userInfo");
            window.location.href = "register.html"; // hoặc login.html
        } catch (err) {
            alert(err.message || "Xóa tài khoản thất bại");
        }
    }
});

// --- Logout ---
document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("userInfo");
    window.location.href = "login.html";
});
