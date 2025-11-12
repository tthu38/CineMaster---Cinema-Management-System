// 📄 footer-loader.js
document.addEventListener("DOMContentLoaded", async () => {
    try {
        const res = await fetch("../home/footer.html");
        const html = await res.text();

        // 🔹 Tạo vùng chứa footer nếu chưa có
        let footerEl = document.getElementById("footer-placeholder");
        if (!footerEl) {
            footerEl = document.createElement("div");
            footerEl.id = "footer-placeholder";
            document.body.appendChild(footerEl);
        }

        // 🔹 Gán nội dung footer
        footerEl.innerHTML = html;

        // 🔹 Đặt footer luôn ở cuối trang (sticky nếu nội dung ít)
        footerEl.style.marginTop = "auto";

        // 🔹 Gọi script trong footer (để hiển thị năm hiện tại)
        footerEl.querySelectorAll("script").forEach((oldScript) => {
            const newScript = document.createElement("script");
            if (oldScript.src) {
                newScript.src = oldScript.src;
            } else {
                newScript.textContent = oldScript.textContent;
            }
            document.body.appendChild(newScript);
        });
    } catch (err) {
        console.error("⚠️ Lỗi khi load footer:", err);
    }
});

