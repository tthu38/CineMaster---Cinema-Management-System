const API_URL = "http://localhost:8080/api/v1/news";

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

document.getElementById("create-news-form").onsubmit = async e => {
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
    const img = document.getElementById("imageFile").files[0];
    if (img) formData.append("imageFile", img);

    const res = await fetch(API_URL, { method: "POST", body: formData });
    const result = await res.json();
    if (result.code === 1000) {
        alert("Tạo mới thành công!");
        window.location.href = "listNews.html";
    } else {
        alert("Tạo mới thất bại: " + result.message);
    }
};
