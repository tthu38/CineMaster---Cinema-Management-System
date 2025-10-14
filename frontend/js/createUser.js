import { accountApi } from "./api/accountApi.js";
import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";

document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;

    const form = document.getElementById("createAccountForm");
    const branchSelect = document.getElementById("branch");
    const roleSelect = document.getElementById("roleId");
    const avatarInput = document.getElementById("avatarFile");

    // ===== LẤY THÔNG TIN NGƯỜI DÙNG HIỆN TẠI =====
    const currentRole = localStorage.getItem("role");
    const managerBranchId = localStorage.getItem("branchId");

    // 🚫 STAFF: không được truy cập trang tạo tài khoản
    if (currentRole === "Staff") {
        alert("❌ Bạn không có quyền tạo tài khoản!");
        window.location.href = "viewUser.html";
        return;
    }

    // ===== HIỂN THỊ DANH SÁCH CHI NHÁNH =====
    if (currentRole === "Manager") {
        // Manager chỉ được tạo tài khoản trong chi nhánh của mình
        branchSelect.innerHTML = `<option value="${managerBranchId}">Chi nhánh của bạn (#${managerBranchId})</option>`;
        branchSelect.disabled = true;
    } else {
        // Admin được chọn chi nhánh bất kỳ
        try {
            const branches = await branchApi.getAll();
            branchSelect.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
            branches.forEach(b => {
                const val = b.branchID ?? b.id ?? b.branchId;
                branchSelect.innerHTML += `<option value="${val}">${b.branchName}</option>`;
            });
        } catch (err) {
            console.error("❌ Lỗi khi tải danh sách chi nhánh:", err);
            branchSelect.innerHTML = `<option value="">(Không tải được chi nhánh)</option>`;
        }
    }

    // ===== GIỚI HẠN ROLE THEO NGƯỜI ĐANG ĐĂNG NHẬP =====
    if (currentRole === "Manager") {
        // Manager chỉ được tạo nhân viên (Staff)
        roleSelect.innerHTML = `<option value="3">Nhân viên (Staff)</option>`;
    } else if (currentRole === "Admin") {
        // Admin tạo được tất cả
        roleSelect.innerHTML = `
            <option value="2">Quản lý (Manager)</option>
            <option value="3">Nhân viên (Staff)</option>
            <option value="4">Khách hàng (Customer)</option>
        `;
    }

    // ===== SUBMIT FORM =====
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        // Xác định branchId chính xác
        const branchIdValue = currentRole === "Manager"
            ? parseInt(managerBranchId, 10)
            : parseInt(branchSelect.value, 10);

        if (!branchIdValue || isNaN(branchIdValue)) {
            alert("⚠️ Vui lòng chọn chi nhánh hợp lệ!");
            return;
        }

        // Thu thập dữ liệu từ form
        const accountData = {
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            fullName: document.getElementById("fullname").value.trim(),
            phoneNumber: document.getElementById("phone").value.trim(),
            address: document.getElementById("address").value.trim(),
            roleId: parseInt(roleSelect.value),
            branchId: branchIdValue,
            isActive: document.getElementById("isActive").value === "true"
        };

        const avatarFile = avatarInput.files[0] || null;
        console.log("📦 Dữ liệu gửi đi:", accountData);

        try {
            await accountApi.create(accountData, avatarFile);
            alert("✅ Tạo tài khoản thành công!");
            window.location.href = "viewUser.html";
        } catch (err) {
            console.error("❌ Error creating account:", err);
            alert(err?.message || "❌ Lỗi khi tạo tài khoản!");
        }
    });
});
