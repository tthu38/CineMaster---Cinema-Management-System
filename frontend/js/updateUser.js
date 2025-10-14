import { accountApi } from "./api/accountApi.js";
import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";

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
    const branchSelect = document.getElementById("branchId");

    const currentRole = localStorage.getItem("role");
    const managerBranchId = localStorage.getItem("branchId");
    let accountCache = null;

    /* ==================== LOAD ACCOUNT DETAIL ==================== */
    try {
        const acc = await accountApi.getById(accountId);
        accountCache = acc;
        console.log("📋 Account detail:", acc);

        document.getElementById("accountId").value = acc.accountID;
        document.getElementById("email").value = acc.email || "";
        document.getElementById("fullname").value = acc.fullName || "";
        document.getElementById("phone").value = acc.phoneNumber || "";
        document.getElementById("address").value = acc.address || "";
        document.getElementById("roleId").value = acc.roleId || "";
        document.getElementById("isActive").value = acc.isActive ? "true" : "false";

        if (acc.avatarUrl) {
            avatarPreview.src = acc.avatarUrl.startsWith("http")
                ? acc.avatarUrl
                : `http://localhost:8080${acc.avatarUrl}`;
        } else {
            avatarPreview.src = "https://via.placeholder.com/150?text=No+Image";
        }

        // ==================== LOAD BRANCH LIST ====================
        if (currentRole === "Manager") {
            // 🔒 Manager chỉ được xem chi nhánh của mình
            branchSelect.innerHTML = `<option value="${managerBranchId}">Chi nhánh của bạn (#${managerBranchId})</option>`;
            branchSelect.value = managerBranchId;
            branchSelect.disabled = true;

            // Nếu account này KHÔNG cùng chi nhánh → chặn luôn
            if (acc.branchId !== parseInt(managerBranchId)) {
                alert("❌ Bạn không thể chỉnh sửa nhân viên của chi nhánh khác!");
                window.location.href = "viewUser.html";
                return;
            }
        } else {
            // 👑 Admin: load tất cả chi nhánh
            const branches = await branchApi.getAll();
            branchSelect.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
            branches.forEach(b => {
                const val = b.branchID ?? b.id ?? b.branchId;
                branchSelect.innerHTML += `<option value="${val}">${b.branchName}</option>`;
            });
            branchSelect.value = acc.branchId;
        }

    } catch (err) {
        console.error("❌ Error loading account:", err);
        alert("Không thể tải thông tin account!");
    }

    /* ==================== PREVIEW AVATAR ==================== */
    fileInput.addEventListener("change", e => {
        const file = e.target.files[0];
        if (file) avatarPreview.src = URL.createObjectURL(file);
    });

    /* ==================== SUBMIT UPDATE ==================== */
    confirmUpdateBtn.addEventListener("click", async () => {
        const rawBranchId = branchSelect.value ? parseInt(branchSelect.value) : null;
        const branchIdValue = currentRole === "Manager"
            ? parseInt(managerBranchId, 10)
            : rawBranchId;

        const accountData = {
            email: document.getElementById("email").value.trim(),
            fullName: document.getElementById("fullname").value.trim(),
            phoneNumber: document.getElementById("phone").value.trim(),
            address: document.getElementById("address").value.trim(),
            roleId: parseInt(document.getElementById("roleId").value),
            branchId: branchIdValue,
            isActive: document.getElementById("isActive").value === "true"
        };

        const avatarFile = fileInput.files[0] || null;

        console.log("📦 Sending update:", accountData);

        try {
            await accountApi.update(accountId, accountData, avatarFile);
            alert("✅ Cập nhật thành công!");
            window.location.href = "viewUser.html";
        } catch (err) {
            console.error("❌ Error updating account:", err);
            alert(err.message || "❌ Lỗi khi cập nhật account!");
        }
    });
});
