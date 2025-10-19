// ================= HOME PAGE FIX: HIỂN THỊ DẠNG SÁCH LẬT =================
import { movieApi } from "../js/api/movieApi.js";

const nowContainer = document.getElementById("now-showing");
const soonContainer = document.getElementById("coming-soon");

async function loadMovies() {
    try {
        const [nowRes, soonRes] = await Promise.all([
            movieApi.getNowShowing(),
            movieApi.getComingSoon()
        ]);

        const nowMovies = nowRes?.result || nowRes || [];
        const soonMovies = soonRes?.result || soonRes || [];

        renderBookPages(nowContainer, nowMovies);
        renderBookPages(soonContainer, soonMovies);

        // Gán phim nổi bật (hero)
        if (nowMovies.length > 0) {
            const featured = document.getElementById("featuredMovie");
            featured.innerHTML = `<i class="fas fa-star"></i> ${nowMovies[0].title}`;
        }
    } catch (err) {
        console.error("❌ Lỗi load phim:", err);
        nowContainer.innerHTML = `<div style="color:#aaa;text-align:center;padding:40px;">Không thể tải phim đang chiếu</div>`;
        soonContainer.innerHTML = `<div style="color:#aaa;text-align:center;padding:40px;">Không thể tải phim sắp chiếu</div>`;
    }
}

// ========================== RENDER “BOOK PAGE” ==========================
function renderBookPages(container, movies) {
    container.innerHTML = "";

    if (!Array.isArray(movies) || movies.length === 0) {
        container.innerHTML = `<div style="color:#aaa;text-align:center;padding:40px;">Chưa có phim</div>`;
        return;
    }

    movies.forEach(m => {
        const page = document.createElement("div");
        page.className = "page-container page-flip";

        page.innerHTML = `
            <div class="movie-page">
                <!-- Trang trước -->
                <div class="page-front">
                    <img src="${m.posterUrl || "../assets/default-movie.jpg"}" alt="${m.title}">
                    <div class="page-footer">
                        <div class="movie-title">${m.title}</div>
                        <div class="movie-genre">${m.genre || "Đang cập nhật"}</div>
                    </div>
                </div>

                <!-- Trang sau -->
                <div class="page-back">
                    <div class="back-title">${m.title}</div>
                    <div class="back-info">
                        <p><i class="fas fa-clock"></i> ${m.duration || "??"} phút</p>
                        <p><i class="fas fa-calendar"></i> ${m.releaseDate ? new Date(m.releaseDate).toLocaleDateString("vi-VN") : "Chưa có ngày"}</p>
                        <p><i class="fas fa-tag"></i> ${m.genre || "Đang cập nhật"}</p>
                    </div>
                    <a href="../movie/movieDetail.html?id=${m.movieID}" class="btn-watch">
                        <i class="fas fa-play"></i> XEM NGAY
                    </a>
                </div>
            </div>
        `;

        container.appendChild(page);
    });
}

// ========================== INIT ==========================
document.addEventListener("DOMContentLoaded", loadMovies);
