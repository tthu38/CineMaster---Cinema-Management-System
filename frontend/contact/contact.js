import { contactApi } from "../js/api/contactApi.js";
import { branchApi } from "../js/api/branchApi.js";

document.addEventListener("DOMContentLoaded", async () => {
    // ===== Load header/footer =====
    const headerEl = document.getElementById("header");
    const footerEl = document.getElementById("footer");

    try {
        // Load HTML
        const headerHTML = await (await fetch("../home/customer-header.html")).text();
        const footerHTML = await (await fetch("../home/footer.html")).text();

        headerEl.innerHTML = headerHTML;
        footerEl.innerHTML = footerHTML;

        // ✅ Ép chạy lại <script> trong customer-header.html>
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = headerHTML;
        const scripts = tempDiv.querySelectorAll("script");
        scripts.forEach(oldScript => {
            const newScript = document.createElement("script");
            if (oldScript.src) newScript.src = oldScript.src;
            else newScript.textContent = oldScript.textContent;
            document.body.appendChild(newScript);
        });

    } catch (err) {
        console.error("❌ Lỗi khi load header/footer:", err);
    }

    // ===== Load danh sách chi nhánh =====
    try {
        const branches = await branchApi.getNames();
        console.log("📡 Branches:", branches);
        const select = document.getElementById("branchSelect");
        branches.forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.id || b.branchID;
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
