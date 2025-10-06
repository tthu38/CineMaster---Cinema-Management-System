import { accountApi } from "./api.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("createAccountForm");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const accountData = {
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            fullName: document.getElementById("fullname").value.trim(),
            phoneNumber: document.getElementById("phone").value.trim(),
            address: document.getElementById("address").value.trim(),
            roleId: parseInt(document.getElementById("roleId").value), // role bằng id
            branchId: document.getElementById("branch")?.value ? parseInt(document.getElementById("branch").value) : null,
            isActive: document.getElementById("isActive").value === "true"
        };

        const avatarFile = document.getElementById("avatarFile").files[0] || null;

        try {
            await accountApi.create(accountData, avatarFile); // upload file
            alert("✅ Tạo tài khoản thành công!");
            window.location.href = "viewUser.html";
        } catch (err) {
            console.error("❌ Error creating account:", err);
            alert("Lỗi khi tạo tài khoản!");
        }
    });
});
