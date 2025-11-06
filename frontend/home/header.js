document.addEventListener("DOMContentLoaded", () => {
    const authArea = document.getElementById("authArea");
    if (!authArea) return;

    const token = localStorage.getItem("accessToken");
    const name = localStorage.getItem("fullName") || "User";
    const avatar = localStorage.getItem("avatarUrl") || "../assets/default-avatar.png";

    // Nếu chưa đăng nhập
    if (!token) {
        authArea.innerHTML = `
            <button class="btn-auth" onclick="location.href='../user/login.html'">Login</button>
            <button class="btn-auth-outline" onclick="location.href='../user/register.html'">Register</button>
        `;
    } else {
        // Nếu đã đăng nhập
        authArea.innerHTML = `
            <div class="user-info">
                <img src="${avatar}" alt="avatar" class="user-avatar">
                <span class="user-name">${name}</span>
                <button class="btn-logout" title="Log out" onclick="logoutUser()">Log out</button>
            </div>
        `;
    }

    // Highlight menu hiện tại
    const current = window.location.pathname.split("/").pop();
    document.querySelectorAll(".cm-nav .nav-link").forEach(link => {
        if (link.getAttribute("href").includes(current)) {
            link.classList.add("active");
        }
    });
});

function logoutUser() {
    try {
        // 1️⃣ Xóa dữ liệu local & session
        localStorage.removeItem("accessToken");
        localStorage.removeItem("fullName");
        localStorage.removeItem("avatarUrl");
        localStorage.removeItem("userRole");
        sessionStorage.clear();

        // 2️⃣ Nếu có dùng Google Identity (GSI)
        if (window.google && google.accounts && google.accounts.id) {
            google.accounts.id.disableAutoSelect();

            const email = localStorage.getItem("userEmail");
            if (email) {
                google.accounts.id.revoke(email, done => console.log("Revoked GSI:", done));
            }
        }

        // 3️⃣ Chuyển hướng về login
        window.location.href = "../user/login.html";
    } catch (err) {
        console.error("Logout failed:", err);
    }
}
