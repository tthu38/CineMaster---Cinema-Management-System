import { contactApi } from "../js/api/contactApi.js";
import { branchApi } from "../js/api/branchApi.js";

document.addEventListener("DOMContentLoaded", async () => {
    // ===== Load header/footer =====
    const headerHTML = await (await fetch("../home/header.html")).text();
    const footerHTML = await (await fetch("../home/footer.html")).text();
    document.getElementById("header").innerHTML = headerHTML;
    document.getElementById("footer").innerHTML = footerHTML;

    const headerScript = document.createElement("script");
    headerScript.src = "../home/header.js";
    document.body.appendChild(headerScript);

    // ===== Load danh sách chi nhánh =====
    try {
        const branches = await branchApi.getNames(); // ✅ dùng /names thay vì getAll()
        console.log("📡 Branches:", branches);
        const select = document.getElementById("branchSelect");

        branches.forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.id || b.branchID; // xử lý cả 2 dạng
            opt.textContent = b.branchName;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error("❌ Không tải được danh sách chi nhánh:", e);
    }


    // ===== Submit form =====
    const form = document.getElementById("contactForm");
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const data = Object.fromEntries(new FormData(form).entries());
        data.branchId = Number(data.branchId) || null;
        console.log("📤 Sending data:", data);

        try {
            await contactApi.create(data);
            Swal.fire("✅ Gửi thành công!", "Yêu cầu của bạn đã được gửi đến chi nhánh.", "success");
            form.reset();
            document.getElementById("charCount").textContent = "0 / 500 ký tự";
        } catch (err) {
            console.error("❌ Error submitting contact:", err);
            Swal.fire("❌ Lỗi", err.message || "Không thể gửi, vui lòng thử lại sau.", "error");
        }
    });
});
