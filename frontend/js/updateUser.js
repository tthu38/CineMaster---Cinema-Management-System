import { accountApi, requireAuth } from "./api.js";

document.addEventListener("DOMContentLoaded", async () => {
    const token = requireAuth();
    if (!token) return;

    const urlParams = new URLSearchParams(window.location.search);
    const accountId = urlParams.get("id");
    if (!accountId) {
        alert("❌ Không tìm thấy ID account");
        window.location.href = "viewUser.html";
        return;
    }

    const form = document.getElementById("updateAccountForm");
    const avatarPreview = document.getElementById("currentAvatar");
    const fileInput = document.getElementById("avatarFile");
    const confirmUpdateBtn = document.getElementById("confirmUpdateButton");

    // ===== Load dữ liệu account =====
    try {
        const acc = await accountApi.getById(accountId);
        console.log("Account detail:", acc);

        document.getElementById("accountId").value = acc.accountID;
        document.getElementById("email").value = acc.email || "";
        document.getElementById("fullname").value = acc.fullName || "";
        document.getElementById("phone").value = acc.phoneNumber || "";
        document.getElementById("address").value = acc.address || "";

        document.getElementById("roleId").value = acc.roleId || "";
        document.getElementById("branchId").value = acc.branchId || "";
        document.getElementById("isActive").value = acc.isActive ? "true" : "false";

        if (acc.avatarUrl) {
            avatarPreview.src = acc.avatarUrl.startsWith("http")
                ? acc.avatarUrl
                : `http://localhost:8080${acc.avatarUrl}`;
        } else {
            avatarPreview.src = "https://via.placeholder.com/150?text=No+Image";
        }
    } catch (err) {
        console.error("❌ Error loading account:", err);
        alert("Không thể tải thông tin account");
    }

    // ===== Preview avatar khi chọn ảnh mới =====
    fileInput.addEventListener("change", (e) => {
        const file = e.target.files[0];
        if (file) {
            avatarPreview.src = URL.createObjectURL(file);
        }
    });

    // ===== Submit update khi bấm "Đồng ý" trong modal =====
    confirmUpdateBtn.addEventListener("click", async () => {
        const accountData = {
            email: document.getElementById("email").value.trim(),
            fullName: document.getElementById("fullname").value.trim(),
            phoneNumber: document.getElementById("phone").value.trim(),
            address: document.getElementById("address").value.trim(),
            roleId: parseInt(document.getElementById("roleId").value),
            branchId: document.getElementById("branchId").value
                ? parseInt(document.getElementById("branchId").value)
                : null,
            isActive: document.getElementById("isActive").value === "true",
        };

        const avatarFile = fileInput.files[0] || null;

        try {
            await accountApi.update(accountId, accountData, avatarFile);
            alert("Cập nhật thành công!");

            // Nếu đang mở trong iframe (tức là trong dashboard)
            if (window.top !== window.self && window.top.loadIframe) {
                window.top.loadIframe("../user/viewUser.html");
            } else {
                // nếu chạy độc lập (test riêng file updateUser.html)
                window.location.href = "viewUser.html";
            }
        } catch (err) {
            console.error("Error updating account:", err);
            alert("Lỗi khi cập nhật account!");
        }

    });
});
