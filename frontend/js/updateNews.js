import { handleResponse, API_BASE_URL } from "./api.js";
import { newsApi } from "./api/newsApi.js";
import { getValidToken } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

const params = new URLSearchParams(window.location.search);
const newsId = params.get("id");

if (!newsId) {
    Swal.fire("Thiếu ID!", "Không tìm thấy ID tin tức cần cập nhật.", "warning")
        .then(() => window.location.href = "listNews.html");
}

document.getElementById("add-detail").onclick = () => addDetailForm();

function addDetailForm(detail = {}) {
    const div = document.createElement("div");
    div.className = "detail-item border border-info p-3 mb-3";
    div.innerHTML = `
        <label>Tiêu đề Section</label>
        <input class="form-control mb-2 section-title" value="${detail.sectionTitle || ''}">
        <label>Nội dung Section</label>
        <textarea class="form-control mb-2 section-content">${detail.sectionContent || ''}</textarea>
        <label>Ảnh Section</label>
        <input type="text" class="form-control mb-2 section-img" value="${detail.imageUrl || ''}" placeholder="URL ảnh hoặc upload">
        <input type="file" class="form-control mb-2 section-file">
        <label>Thứ tự hiển thị</label>
        <input type="number" class="form-control mb-2 section-order" value="${detail.displayOrder || ''}">
        <button type="button" class="btn btn-sm btn-danger remove-detail">Xóa</button>
    `;
    div.querySelector(".remove-detail").onclick = () => div.remove();

    const fileInput = div.querySelector(".section-file");
    const urlInput = div.querySelector(".section-img");
    fileInput.onchange = async () => {
        const file = fileInput.files[0];
        if (!file) return;
        const fd = new FormData();
        fd.append("file", file);
        const token = getValidToken();
        try {
            const res = await fetch(`${API_BASE_URL}/users/news-image`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
                body: fd,
            });
            const data = await handleResponse(res);
            if (data) {
                urlInput.value = data;
                Swal.fire("Tải lên thành công!", "Ảnh section đã được cập nhật.", "success");
            }
        } catch {
            Swal.fire("Lỗi", "Không thể upload ảnh section!", "error");
        }
    };

    document.getElementById("details-container").appendChild(div);
}

// =========================
// 📦 Load dữ liệu hiện tại
// =========================
(async () => {
    try {
        const n = await newsApi.getById(newsId);
        document.getElementById("title").value = n.title;
        document.getElementById("content").value = n.content;
        document.getElementById("category").value = n.category;
        document.getElementById("remark").value = n.remark || "";
        document.getElementById("active").value = n.active ? "true" : "false";
        (n.details || []).forEach(d => addDetailForm(d));
    } catch (err) {
        console.error("❌ Lỗi tải tin:", err);
        Swal.fire("Lỗi", "Không thể tải dữ liệu tin tức.", "error");
    }
})();

// =========================
// 💾 Submit cập nhật
// =========================
document.getElementById("update-news-form").onsubmit = async e => {
    e.preventDefault();

    const newsData = {
        title: document.getElementById("title").value,
        content: document.getElementById("content").value,
        category: document.getElementById("category").value,
        remark: document.getElementById("remark").value,
        active: document.getElementById("active").value === "true",
        details: [],
    };

    document.querySelectorAll("#details-container .detail-item").forEach(div => {
        newsData.details.push({
            sectionTitle: div.querySelector(".section-title").value,
            sectionContent: div.querySelector(".section-content").value,
            imageUrl: div.querySelector(".section-img").value,
            displayOrder: parseInt(div.querySelector(".section-order").value) || 0,
        });
    });

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(newsData)], { type: "application/json" }));
    const img = document.getElementById("imageFile")?.files[0];
    if (img) formData.append("imageFile", img);

    try {
        await newsApi.update(newsId, newsData, img);
        await Swal.fire("Thành công!", "Tin tức đã được cập nhật.", "success");
        window.location.href = "listNews.html";
    } catch (err) {
        console.error("❌", err);
        Swal.fire("Lỗi", err.message || "Không thể cập nhật tin tức.", "error");
    }
};
