// ✅ CineMaster • createNews.js
import { API_BASE_URL, handleResponse } from "./api.js";
import { newsApi } from "./api/newsApi.js";
import { getValidToken, requireAuth } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

// ==========================
// 🔹 Khi trang load
// ==========================
document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;

    document.getElementById("add-detail").onclick = () => addDetailForm();
    document.getElementById("create-news-form").onsubmit = createNewsHandler;
});

// ==========================
// 🔹 Xử lý Submit tạo tin mới
// ==========================
async function createNewsHandler(e) {
    e.preventDefault();

    const newsData = {
        title: document.getElementById("title").value.trim(),
        content: document.getElementById("content").value.trim(),
        category: document.getElementById("category").value.trim(),
        remark: document.getElementById("remark").value.trim(),
        active: document.getElementById("active").checked,
        details: [],
    };

    // 🔹 Lấy các section chi tiết
    document.querySelectorAll("#details-container .detail-item").forEach(div => {
        newsData.details.push({
            sectionTitle: div.querySelector(".section-title").value.trim(),
            sectionContent: div.querySelector(".section-content").value.trim(),
            imageUrl: div.querySelector(".section-img").value.trim(),
            displayOrder: parseInt(div.querySelector(".section-order").value) || 0,
        });
    });

    // 🔹 Đóng gói multipart/form-data
    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(newsData)], { type: "application/json" }));
    const img = document.getElementById("imageFile").files[0];
    if (img) formData.append("imageFile", img);

    try {
        await newsApi.create(formData);
        await Swal.fire("Thành công!", "Tin tức đã được tạo!", "success");
        window.location.href = "listNews.html";
    } catch (err) {
        console.error("❌ Lỗi tạo tin:", err);
        Swal.fire("Lỗi", err.message || "Không thể tạo tin tức.", "error");
    }
}

// ==========================
// 🔹 Tạo form chi tiết Section
// ==========================
function addDetailForm(detail = {}) {
    const div = document.createElement("div");
    div.className = "detail-item border border-info p-3 mb-3 rounded shadow-sm";
    div.innerHTML = `
        <label>Tiêu đề Section</label>
        <input class="form-control mb-2 section-title" value="${detail.sectionTitle || ""}">
        <label>Nội dung Section</label>
        <textarea class="form-control mb-2 section-content" rows="3">${detail.sectionContent || ""}</textarea>
        <label>Ảnh Section</label>
        <div class="input-group mb-2">
            <input type="text" class="form-control section-img" value="${detail.imageUrl || ""}" placeholder="URL ảnh hoặc upload">
            <input type="file" class="form-control section-file">
        </div>
        <label>Thứ tự hiển thị</label>
        <input type="number" class="form-control mb-2 section-order" value="${detail.displayOrder || ""}">
        <button type="button" class="btn btn-sm btn-danger remove-detail">
            <i class="fas fa-trash"></i> Xóa Section
        </button>
    `;

    // ❌ Xóa section
    div.querySelector(".remove-detail").onclick = () => div.remove();

    // 📸 Upload ảnh section
    const fileInput = div.querySelector(".section-file");
    const urlInput = div.querySelector(".section-img");
    fileInput.onchange = async () => {
        const file = fileInput.files[0];
        if (!file) return;
        const token = getValidToken();
        const fd = new FormData();
        fd.append("file", file);
        try {
            const res = await fetch(`${API_BASE_URL}/users/news-image`, {
                method: "POST",
                headers: token ? { Authorization: `Bearer ${token}` } : {},
                body: fd,
            });
            const data = await handleResponse(res);
            urlInput.value = data;
            Swal.fire("Thành công!", "Ảnh section đã được tải lên!", "success");
        } catch (err) {
            console.error("❌ Lỗi upload:", err);
            Swal.fire("Lỗi", "Không thể upload ảnh section!", "error");
        }
    };

    document.getElementById("details-container").appendChild(div);
}
