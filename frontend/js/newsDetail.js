// ================================
// 🎬 CINE MASTER • NEWS DETAIL PAGE
// ================================

const API_BASE_URL = "http://localhost:8080/api/v1";
const newsId = new URLSearchParams(window.location.search).get("id");

// 🔹 Load chi tiết tin tức
async function loadNewsDetail() {
    try {
        const res = await fetch(`${API_BASE_URL}/news/${newsId}`);
        const data = await res.json();

        if (!data || data.code !== 1000) {
            console.error("❌ Không thể tải dữ liệu tin tức:", data?.message);
            document.getElementById("news-content").innerHTML =
                `<p class="text-danger text-center mt-5">Không tìm thấy tin tức.</p>`;
            return;
        }

        const n = data.result;
        document.title = "CineMaster • " + n.title;

        // 🔹 Hiển thị tiêu đề breadcrumb
        const breadcrumbTitle = document.getElementById("breadcrumb-title");
        if (breadcrumbTitle) breadcrumbTitle.innerText = n.title;

        // 🔹 Hiển thị nội dung chính của tin tức
        const container = document.getElementById("news-content");
        if (!container) {
            console.error("❌ Không tìm thấy phần tử #news-content trong HTML.");
            return;
        }

        // 🔹 Sắp xếp chi tiết tin tức theo DisplayOrder tăng dần (nếu có)
        if (Array.isArray(n.details) && n.details.length > 0) {
            n.details.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
        }

        // 🔹 Render toàn bộ nội dung tin
        container.innerHTML = `
            <h2 class="news-main-title">${n.title}</h2>

            <div class="news-meta mb-3">
                <i class="far fa-calendar"></i> 
                ${n.publishDate ? new Date(n.publishDate).toLocaleDateString() : "Không xác định"}
                &nbsp;|&nbsp; <i class="fas fa-eye"></i> ${n.views ?? 0} lượt xem
            </div>

            <img src="${n.imageUrl || '/assets/default-news.jpg'}" alt="${n.title}" class="main-image">

            <div class="news-main-content mt-4">
                ${n.content || "<i>Không có nội dung chính.</i>"}
            </div>

            ${
            Array.isArray(n.details) && n.details.length > 0
                ? n.details
                    .map(
                        (d) => `
                            <div class="news-detail-section">
                                <h3 class="section-title">${d.sectionTitle || "Chi tiết"}</h3>
                                ${
                            d.imageUrl
                                ? `<img src="${d.imageUrl}" class="section-image" alt="${d.sectionTitle || ""}">`
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
        console.error("🔥 Lỗi khi fetch dữ liệu tin tức:", err);
        const container = document.getElementById("news-content");
        if (container)
            container.innerHTML = `<p class="text-danger text-center mt-5">Lỗi tải dữ liệu tin tức.</p>`;
    }
}

// 🔹 Khởi chạy sau khi DOM sẵn sàng
document.addEventListener("DOMContentLoaded", loadNewsDetail);
