const API_BASE = "http://localhost:8080/api/v1/movies";
const movieList = document.getElementById("movie-list");

const btnNow = document.getElementById("btn-now-showing");
const btnSoon = document.getElementById("btn-coming-soon");
const btnAll = document.getElementById("btn-all");

let currentFilter = "Now Showing"; // Mặc định hiển thị phim đang chiếu

// ========================== LOAD MOVIES ==========================
async function loadMovies(status = "") {
    try {
        let url = API_BASE;
        if (status && status !== "All") {
            url += `?status=${encodeURIComponent(status)}`;
        }

        const res = await fetch(url);
        const data = await res.json();

        if (data.code !== 1000) throw new Error(data.message);
        renderMovies(data.result);
    } catch (err) {
        console.error(err);
        movieList.innerHTML = `
            <div class="col-12 text-center text-danger">
                <p>Không thể tải danh sách phim!</p>
            </div>
        `;
        Swal.fire("Lỗi", "Không thể tải danh sách phim!", "error");
    }
}

// ========================== RENDER MOVIES ==========================
function renderMovies(movies) {
    movieList.innerHTML = "";

    if (!movies || movies.length === 0) {
        movieList.innerHTML = `
            <p class="text-center text-muted fst-italic">Hiện chưa có phim nào trong danh mục này.</p>
        `;
        return;
    }

    movies.forEach(m => {
        const card = document.createElement("div");
        card.className = "movie-card";

        // Badge trạng thái (đang chiếu hoặc sắp chiếu)
        const badgeClass = m.status === "Coming Soon" ? "soon" : "";
        const badgeText = m.status || "Unknown";

        card.innerHTML = `
            <div class="status-badge ${badgeClass}">${badgeText}</div>
            <div class="image-wrapper">
                <img src="${m.posterUrl || '../image/default_movie.jpg'}" alt="${m.title}">
            </div>
            <div class="movie-info">
                <h5>${m.title}</h5>
                <p>${m.genre || "Chưa cập nhật thể loại"}</p>
            </div>
        `;

        // Click vào card -> chuyển đến trang chi tiết phim
        card.addEventListener("click", () => {
            window.location.href = `movieDetail.html?id=${m.movieId}`;
        });

        movieList.appendChild(card);
    });
}

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
    loadMovies(""); // Tải tất cả
});

// ========================== INIT ==========================
loadMovies(currentFilter);
