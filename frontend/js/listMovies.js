import { moviesApi } from "../js/api.js";

const tbody = document.getElementById("movies-body");
const pageNumbers = document.getElementById("page-numbers");
let movieChart = null;
let allMovies = [];
let currentPage = 1;
const pageSize = 10;

// Sắp xếp phim theo status
function sortMovies(movies) {
    const order = { "Now Showing": 1, "Coming Soon": 2, "Ended": 3 };
    return movies.sort((a, b) => (order[a.status] || 99) - (order[b.status] || 99));
}

// Render phim theo trang
function renderMovies(movies, page = 1) {
    tbody.innerHTML = "";
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    const pageMovies = movies.slice(start, end);

    pageMovies.forEach(m => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><img src="${m.posterUrl || ''}" class="movie-poster"/></td>
            <td title="${m.title}" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                ${m.title}
            </td>
            <td>${m.genre}</td>
            <td>${m.duration} phút</td>
            <td>${m.releaseDate}</td>
            <td>${m.director}</td>
            <td>${m.status}</td>
            <td style="min-width:160px;">
                <div class="btn-group-action">
                    ${m.status !== "Ended" ? `
                        <button class="btn btn-sm btn-primary"
                            onclick="window.location.href='updateMovie.html?id=${m.movieId}'">
                            <i class="fa fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteMovie(${m.movieId})">
                            <i class="fa fa-trash"></i>
                        </button>
                    ` : `
                        <button class="btn btn-sm btn-success" onclick="restoreMovie(${m.movieId})">
                            <i class="fa fa-rotate-left"></i>
                        </button>
                    `}
                    <button class="btn btn-sm btn-warning" onclick="showTrailer('${m.trailerUrl}')">
                        <i class="fa fa-play"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });

    renderPagination(movies);
}

// Render số trang
function renderPagination(movies) {
    const totalPages = Math.ceil(movies.length / pageSize);
    pageNumbers.innerHTML = "";

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement("button");
        btn.textContent = i;
        btn.className = `btn btn-sm ${i === currentPage ? "btn-primary" : "btn-secondary"} me-1`;
        btn.onclick = () => {
            currentPage = i;
            renderMovies(movies, currentPage);
        };
        pageNumbers.appendChild(btn);
    }
}

async function loadMovies() {
    try {
        allMovies = await moviesApi.getAll();
        allMovies = sortMovies(allMovies);
        currentPage = 1;
        renderMovies(allMovies, currentPage);
        updateStats(allMovies);
    } catch (err) {
        console.error("Load movies error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tải danh sách phim!", "error");
    }
}

window.deleteMovie = async function(id) {
    Swal.fire({
        title: "Xác nhận",
        text: "Chuyển phim này sang Ended?",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Đồng ý",
        cancelButtonText: "Hủy"
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                await moviesApi.remove(id);
                Swal.fire("Thành công", "Phim đã được chuyển sang Ended", "success")
                    .then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể xóa phim!", "error");
            }
        }
    });
};

window.restoreMovie = async function(id) {
    Swal.fire({
        title: "Khôi phục phim",
        text: "Chọn trạng thái mới",
        icon: "question",
        showDenyButton: true,
        showCancelButton: true,
        confirmButtonText: "Now Showing",
        denyButtonText: "Coming Soon",
        cancelButtonText: "Hủy"
    }).then(async (result) => {
        let newStatus = null;
        if (result.isConfirmed) newStatus = "Now Showing";
        else if (result.isDenied) newStatus = "Coming Soon";

        if (newStatus) {
            try {
                const movie = await moviesApi.getById(id);
                movie.status = newStatus;
                await moviesApi.update(id, movie);
                Swal.fire("Thành công", `Đã khôi phục sang ${newStatus}`, "success")
                    .then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể khôi phục phim!", "error");
            }
        }
    });
};

window.showTrailer = function (url) {
    if (!url) {
        Swal.fire("Lỗi", "Trailer không hợp lệ!", "error");
        return;
    }
    let embedUrl = url.replace("watch?v=", "embed/");
    const videoId = url.split("v=")[1]?.split("&")[0]; // lấy id video
    embedUrl += `?autoplay=1&mute=0&controls=0&modestbranding=1&rel=0&loop=1&playlist=${videoId}`;

    Swal.fire({
        html: `
            <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;border-radius:12px;">
                <iframe 
                    src="${embedUrl}" 
                    frameborder="0" 
                    allow="autoplay; encrypted-media" 
                    allowfullscreen 
                    style="position:absolute;top:0;left:0;width:100%;height:100%;">
                </iframe>
            </div>
        `,
        width: 900,
        background: "transparent",
        showConfirmButton: false,
        showCloseButton: true,
    });
};

function updateStats(movies) {
    const total = movies.length;
    const nowShowing = movies.filter(m => m.status === "Now Showing").length;
    const comingSoon = movies.filter(m => m.status === "Coming Soon").length;

    document.getElementById("total-movies").textContent = total;
    document.getElementById("current-movies").textContent = nowShowing;
    document.getElementById("upcoming-movies").textContent = comingSoon;

    const ctx = document.getElementById("movieStatusChart").getContext("2d");
    if (movieChart) movieChart.destroy();

    movieChart = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Đang chiếu", "Sắp chiếu", "Khác"],
            datasets: [{
                data: [nowShowing, comingSoon, total - nowShowing - comingSoon],
                backgroundColor: ["#0aa3ff", "#22c1ff", "#e50914"]
            }]
        },
        options: {
            plugins: { legend: { labels: { color: "#fff" } } }
        }
    });
}

loadMovies();
