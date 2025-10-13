import { movieApi } from "./api/movieApi.js";

const movieList = document.getElementById("movie-list");
const btnNow = document.getElementById("btn-now-showing");
const btnSoon = document.getElementById("btn-coming-soon");
const btnAll = document.getElementById("btn-all");
const searchInput = document.querySelector(".search-box input");
const searchBtn = document.querySelector(".search-box button");

let currentFilter = "Now Showing"; // Mặc định hiển thị phim đang chiếu
let allMovies = []; // Cache phim để search nhanh

// ========================== LOAD MOVIES ==========================
async function loadMovies(filter = "") {
    try {
        let res;

        if (filter === "Now Showing") {
            res = await movieApi.getNowShowing();
        } else if (filter === "Coming Soon") {
            res = await movieApi.getComingSoon();
        } else {
            res = await movieApi.getAll();
        }

        // ✅ Chuẩn hóa kết quả (vì API có thể trả {code, message, result})
        const movies = res?.result || res || [];
        allMovies = movies;

        renderMovies(movies);
    } catch (err) {
        console.error("❌ Load movies error:", err);
        movieList.innerHTML = `
            <div class="col-12 text-center text-danger">
                <p>Không thể tải danh sách phim!</p>
            </div>
        `;
    }
}

// ========================== RENDER MOVIES ==========================
function renderMovies(movies) {
    movieList.innerHTML = "";

    if (!movies || movies.length === 0) {
        movieList.innerHTML = `
            <p class="text-center text-muted fst-italic">
                Hiện chưa có phim nào trong danh mục này.
            </p>
        `;
        return;
    }

    movies.forEach(m => {
        const card = document.createElement("div");
        card.className = "movie-card";

        const badgeClass = m.status === "Coming Soon" ? "soon" : "";
        const badgeText = m.status || "Unknown";

        card.innerHTML = `
            <div class="status-badge ${badgeClass}">${badgeText}</div>
            <div class="image-wrapper">
                <img src="${m.posterUrl || "../image/default_movie.jpg"}" alt="${m.title}">
                <div class="action-overlay">
                    ${
            m.status === "Coming Soon"
                ? `<button class="btn btn-lg btn-book-now btn-soon">XEM THÔNG BÁO</button>`
                : `<button class="btn btn-lg btn-book-now">ĐẶT VÉ NGAY</button>`
        }
                </div>
            </div>
            <div class="movie-info">
                <h5>${m.title}</h5>
                <p class="genre-list">${m.genre || "Chưa cập nhật thể loại"}</p>
            </div>
        `;

        // ✅ Khi click → chuyển sang trang chi tiết
        card.addEventListener("click", () => {
            window.location.href = `movieDetail.html?id=${m.movieID}`;
        });

        movieList.appendChild(card);
    });
}

// ========================== SEARCH FUNCTION ==========================
function searchMovies(keyword) {
    const k = keyword.toLowerCase().trim();

    if (k === "") {
        renderMovies(allMovies);
        return;
    }

    const filtered = allMovies.filter(m =>
        (m.title || "").toLowerCase().includes(k)
    );
    renderMovies(filtered);
}

searchInput.addEventListener("input", e => searchMovies(e.target.value));
searchBtn.addEventListener("click", () => searchMovies(searchInput.value));

// ========================== BUTTON FILTERS ==========================
function setActiveButton(activeBtn) {
    [btnNow, btnSoon, btnAll].forEach(btn => {
        btn.classList.remove("btn-custom-active");
        btn.classList.add("btn-custom-outline");
    });
    activeBtn.classList.add("btn-custom-active");
    activeBtn.classList.remove("btn-custom-outline");
}

btnNow.addEventListener("click", () => {
    currentFilter = "Now Showing";
    setActiveButton(btnNow);
    loadMovies(currentFilter);
});

btnSoon.addEventListener("click", () => {
    currentFilter = "Coming Soon";
    setActiveButton(btnSoon);
    loadMovies(currentFilter);
});

btnAll.addEventListener("click", () => {
    currentFilter = "All";
    setActiveButton(btnAll);
    loadMovies("");
});

// ========================== INIT ==========================
document.addEventListener("DOMContentLoaded", () => {
    loadMovies(currentFilter);
});
