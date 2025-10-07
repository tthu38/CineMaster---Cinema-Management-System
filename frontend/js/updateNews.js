const API_URL = "http://localhost:8080/api/v1/news";
const params = new URLSearchParams(window.location.search);
const newsId = params.get("id");

// Nút thêm Section
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

    // Upload file ảnh ngay khi chọn
    const fileInput = div.querySelector(".section-file");
    const urlInput = div.querySelector(".section-img");
    fileInput.onchange = async () => {
        const file = fileInput.files[0];
        if (file) {
            const fd = new FormData();
            fd.append("file", file);
            try {
                const res = await fetch("http://localhost:8080/api/v1/users/news-image", {
                    method: "POST",
                    body: fd
                });
                const data = await res.json();
                if (data.code === 1000) {
                    urlInput.value = data.result;
                } else {
                    alert("Upload ảnh detail thất bại: " + data.message);
                }
            } catch {
                alert("Lỗi kết nối khi upload ảnh detail");
            }
        }
    };

    document.getElementById("details-container").appendChild(div);
}

// Load dữ liệu news + details để edit
fetch(`${API_URL}/${newsId}`)
    .then(r => r.json())
    .then(data => {
        const n = data.result;
        document.getElementById("title").value = n.title;
        document.getElementById("content").value = n.content;
        document.getElementById("category").value = n.category;
        document.getElementById("remark").value = n.remark ?? "";
        document.getElementById("active").value = n.active ? "true" : "false";

        if (n.details) {
            n.details.forEach(d => addDetailForm(d));
        }
    })
    .catch(err => alert("Lỗi tải dữ liệu tin tức: " + err));

// Submit cập nhật News
document.getElementById("update-news-form").onsubmit = async e => {
    e.preventDefault();

    const newsData = {
        title: document.getElementById("title").value,
        content: document.getElementById("content").value,
        category: document.getElementById("category").value,
        remark: document.getElementById("remark").value,
        active: document.getElementById("active").value === "true",
        details: []
    };

    document.querySelectorAll("#details-container .detail-item").forEach(div => {
        newsData.details.push({
            sectionTitle: div.querySelector(".section-title").value,
            sectionContent: div.querySelector(".section-content").value,
            imageUrl: div.querySelector(".section-img").value,
            displayOrder: parseInt(div.querySelector(".section-order").value) || null
        });
    });

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(newsData)], { type: "application/json" }));
    const img = document.getElementById("imageFile")?.files[0];
    if (img) formData.append("imageFile", img);

    try {
        const res = await fetch(`${API_URL}/${newsId}`, { method: "PUT", body: formData });
        const result = await res.json();
        if (result.code === 1000) {
            alert("Cập nhật thành công!");
            window.location.href = "listNews.html";
        } else {
            alert("Cập nhật thất bại: " + result.message);
        }
    } catch (err) {
        alert("Lỗi kết nối server khi cập nhật");
    }
};
