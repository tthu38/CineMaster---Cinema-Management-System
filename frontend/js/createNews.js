import { newsApi, API_BASE_URL, getValidToken, handleResponse } from "./api.js";

document.getElementById("add-detail").onclick = () => addDetailForm();

// ================================
// 🔹 Tạo form chi tiết Section
// ================================
function addDetailForm(detail = {}) {
    const div = document.createElement("div");
    div.className = "detail-item border border-info p-3 mb-3 rounded";
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

    // ❌ Nút xóa Section
    div.querySelector(".remove-detail").onclick = () => div.remove();

    // 📸 Upload ảnh section
    const fileInput = div.querySelector(".section-file");
    const urlInput = div.querySelector(".section-img");
    fileInput.onchange = async () => {
        const file = fileInput.files[0];
        if (file) {
            const token = getValidToken();
            const fd = new FormData();
            fd.append("file", file);
            try {
                const res = await fetch(`${API_BASE_URL}/users/news-image`, {
                    method: "POST",
                    headers: {
                        Authorization: token ? `Bearer ${token}` : undefined,
                    },
                    body: fd,
                });
                const data = await handleResponse(res);
                urlInput.value = data; // backend trả link ảnh
            } catch (err) {
                console.error(err);
                alert("❌ Lỗi khi upload ảnh section: " + err.message);
            }
        }
    };

    document.getElementById("details-container").appendChild(div);
}

// ================================
// 🔹 Submit tạo mới tin tức
// ================================
document.getElementById("create-news-form").onsubmit = async (e) => {
    e.preventDefault();

    // ✅ Gom dữ liệu từ form
    const newsData = {
        title: document.getElementById("title").value.trim(),
        content: document.getElementById("content").value.trim(),
        category: document.getElementById("category").value.trim(),
        remark: document.getElementById("remark").value.trim(),
        active: document.getElementById("active").checked, // ✅ checkbox
        details: [],
    };

    document.querySelectorAll("#details-container .detail-item").forEach((div) => {
        newsData.details.push({
            sectionTitle: div.querySelector(".section-title").value.trim(),
            sectionContent: div.querySelector(".section-content").value.trim(),
            imageUrl: div.querySelector(".section-img").value.trim(),
            displayOrder:
                parseInt(div.querySelector(".section-order").value) || null,
        });
    });

    // ✅ Chuẩn bị FormData để gửi multipart/form-data
    const formData = new FormData();
    formData.append(
        "data",
        new Blob([JSON.stringify(newsData)], { type: "application/json" })
    );

    const img = document.getElementById("imageFile").files[0];
    if (img) formData.append("imageFile", img);

    try {
        await newsApi.create(formData); // ✅ Gọi qua module đã export
        alert("✅ Tạo tin tức thành công!");
        window.location.href = "listNews.html";
    } catch (err) {
        console.error("❌ Lỗi tạo tin:", err);
        alert("❌ Tạo mới thất bại: " + err.message);
    }
};
