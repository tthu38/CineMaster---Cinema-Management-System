// ================================
// 🎬 CINE MASTER • NEWS DETAIL PAGE
// ================================
import { newsApi } from "./api.js";

// Lấy ID tin tức từ URL (?id=)
const newsId = new URLSearchParams(window.location.search).get("id");

// 🔹 Hàm load chi tiết tin tức
async function loadNewsDetail() {
    const container = document.getElementById("news-content");
    const breadcrumbTitle = document.getElementById("breadcrumb-title");

    if (!newsId) {
        container.innerHTML = `<p class="text-danger text-center mt-5">❌ Thiếu ID tin tức.</p>`;
        return;
    }

    try {
        // ✅ Gọi API chuẩn qua newsApi
        const n = await newsApi.getById(newsId);
        if (!n) {
            container.innerHTML = `<p class="text-danger text-center mt-5">Không tìm thấy tin tức.</p>`;
            return;
        }

        // Cập nhật tiêu đề trang
        document.title = `CineMaster • ${n.title}`;
        if (breadcrumbTitle) breadcrumbTitle.textContent = n.title;

        // 🔹 Sắp xếp section theo thứ tự hiển thị
        if (Array.isArray(n.details) && n.details.length > 0) {
            n.details.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
        }

        // 🔹 Render nội dung chính
        container.innerHTML = `
            <h2 class="news-main-title">${n.title}</h2>

            <div class="news-meta mb-3 text-muted">
                <i class="far fa-calendar"></i>
                ${n.publishDate ? new Date(n.publishDate).toLocaleDateString() : "Không xác định"}
                &nbsp;|&nbsp; 
                <i class="fas fa-eye"></i> ${n.views ?? 0} lượt xem
            </div>

            <img src="${n.imageUrl || "/assets/default-news.jpg"}" alt="${n.title}" class="main-image mb-4">

            <div class="news-main-content">
                ${n.content || "<i>Không có nội dung chính.</i>"}
            </div>

            ${
            Array.isArray(n.details) && n.details.length > 0
                ? n.details
                    .map(
                        (d) => `
                            <div class="news-detail-section mt-5">
                                <h3 class="section-title">${d.sectionTitle || "Chi tiết"}</h3>
                                ${
                            d.imageUrl
                                ? `<img src="${d.imageUrl}" class="section-image my-3" alt="${d.sectionTitle || ""}">`
                                : ""
                        }
                                <div class="section-content">
                                    ${d.sectionContent || ""}
                                </div>
                            </div>
                        `
                    )
                    .join("")
                : ""
        }
        `;
    } catch (err) {
        console.error("🔥 Lỗi khi tải dữ liệu tin tức:", err);
        container.innerHTML = `<p class="text-danger text-center mt-5">Lỗi tải dữ liệu tin tức.</p>`;
    }
}

// 🔹 Khởi chạy sau khi DOM sẵn sàng
document.addEventListener("DOMContentLoaded", loadNewsDetail);
