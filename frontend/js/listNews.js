import { newsApi } from "./api/newsApi.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

// ==============================
// 🔹 LOAD NEWS LIST
// ==============================
async function loadNews(category = "") {
    const newsList = document.getElementById("news-list");
    newsList.innerHTML = `<div class="text-center w-100 py-5 text-muted">Đang tải tin tức...</div>`;

    try {
        const data = await newsApi.getAll(category);
        newsList.innerHTML = "";

        if (!data || data.length === 0) {
            newsList.innerHTML = `<div class="text-center w-100 py-5 text-muted">Không có tin tức nào.</div>`;
            return;
        }

        data.forEach(news => {
            const col = document.createElement("div");
            col.className = "col-md-6 col-lg-4 d-flex";

            const actions = news.active
                ? `
                    <a href="updateNews.html?id=${news.newsID}" class="btn btn-sm btn-warning">
                        <i class="fas fa-edit"></i> Sửa
                    </a>
                    <button class="btn btn-sm btn-danger delete-btn" data-id="${news.newsID}">
                        <i class="fas fa-trash"></i> Xóa
                    </button>`
                : `
                    <button class="btn btn-sm btn-success restore-btn" data-id="${news.newsID}">
                        <i class="fas fa-undo"></i> Khôi phục
                    </button>`;

            col.innerHTML = `
                <div class="news-card flex-fill">
                    <img src="${news.imageUrl || '/assets/img/no-image.png'}" alt="${news.title}">
                    <div class="news-card-body d-flex flex-column">
                        <div class="flex-grow-1">
                            <h5>${news.title}</h5>
                            <p><small>${new Date(news.publishDate).toLocaleDateString("vi-VN")}</small></p>
                            <p>${news.content ? news.content.substring(0, 120) : ""}...</p>
                            <a href="newsDetail.html?id=${news.newsID}" class="read-more">Xem chi tiết</a>
                        </div>
                        <div class="news-actions mt-3">${actions}</div>
                    </div>
                </div>`;
            newsList.appendChild(col);
        });

        // ========== SỰ KIỆN XÓA ==========
        document.querySelectorAll(".delete-btn").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = btn.dataset.id;

                const confirm = await Swal.fire({
                    title: "Xác nhận xóa?",
                    text: "Bạn có chắc muốn xóa tin tức này?",
                    icon: "warning",
                    showCancelButton: true,
                    confirmButtonText: "Xóa",
                    cancelButtonText: "Hủy",
                    confirmButtonColor: "#e50914",
                    cancelButtonColor: "#6c757d"
                });

                if (!confirm.isConfirmed) return;

                try {
                    await newsApi.delete(id);
                    await Swal.fire("Thành công!", "Tin tức đã được xóa.", "success");
                    loadNews(category);
                } catch (err) {
                    console.error("❌ Lỗi khi xóa tin:", err);
                    Swal.fire("Lỗi", "Không thể xóa tin tức.", "error");
                }
            });
        });

        // ========== SỰ KIỆN KHÔI PHỤC ==========
        document.querySelectorAll(".restore-btn").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = btn.dataset.id;

                const confirm = await Swal.fire({
                    title: "Khôi phục tin tức?",
                    text: "Bạn có chắc muốn khôi phục tin này?",
                    icon: "question",
                    showCancelButton: true,
                    confirmButtonText: "Khôi phục",
                    cancelButtonText: "Hủy",
                    confirmButtonColor: "#00bfff",
                    cancelButtonColor: "#6c757d"
                });

                if (!confirm.isConfirmed) return;

                try {
                    await newsApi.restore(id);
                    await Swal.fire("Thành công!", "Tin tức đã được khôi phục.", "success");
                    loadNews(category);
                } catch (err) {
                    console.error("❌ Lỗi khi khôi phục:", err);
                    Swal.fire("Lỗi", "Không thể khôi phục tin tức.", "error");
                }
            });
        });

    } catch (error) {
        console.error("❌ Lỗi khi tải tin tức:", error);
        Swal.fire("Lỗi", "Không thể tải danh sách tin tức!", "error");
        newsList.innerHTML = `<div class="text-center w-100 py-5 text-danger">Không thể tải danh sách tin tức!</div>`;
    }
}

document.querySelectorAll(".category-link").forEach(link => {
    link.addEventListener("click", e => {
        e.preventDefault();
        document.querySelectorAll(".category-link").forEach(l => l.classList.remove("active"));
        link.classList.add("active");
        loadNews(link.dataset.category);
    });
});

loadNews();
