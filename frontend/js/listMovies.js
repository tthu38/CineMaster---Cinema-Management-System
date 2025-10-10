// ===========================
// 🎬 CineMaster • List Movies
// ===========================

import { movieApi } from "./api.js";

const tbody = document.getElementById("movies-body");
const pageNumbers = document.getElementById("page-numbers");
let movieChart = null;
let allMovies = [];
let currentPage = 1;
const pageSize = 10;

// 🔹 Sắp xếp phim theo trạng thái
function sortMovies(movies) {
    const order = { "Now Showing": 1, "Coming Soon": 2, "Ended": 3 };
    return movies.sort((a, b) => (order[a.status] || 99) - (order[b.status] || 99));
}

// 🔹 Render phim theo trang
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
            <td>${m.genre || "-"}</td>
            <td>${m.duration || 0} phút</td>
            <td>${m.releaseDate || "-"}</td>
            <td>${m.director || "-"}</td>
            <td>${m.status || "-"}</td>
            <td style="min-width:160px;">
                <div class="btn-group-action">
                    ${m.status !== "Ended" ? `
                        <button class="btn btn-sm btn-primary"
                            onclick="editMovie(${m.movieID})">
                            <i class="fa fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteMovie(${m.movieID})">
                            <i class="fa fa-trash"></i>
                        </button>
                    ` : `
                        <button class="btn btn-sm btn-success" onclick="restoreMovie(${m.movieID})">
                            <i class="fa fa-rotate-left"></i>
                        </button>
                    `}
                    <button class="btn btn-sm btn-warning" onclick="showTrailer('${m.trailerUrl || ""}')">
                        <i class="fa fa-play"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });

    renderPagination(movies);
}

// 🔹 Render phân trang
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

// 🔹 Load danh sách phim
async function loadMovies() {
    try {
        const res = await movieApi.getAll();
        allMovies = res?.result || res; // ✅ hỗ trợ cả {code, result}
        allMovies = sortMovies(allMovies);
        currentPage = 1;
        renderMovies(allMovies, currentPage);
        updateStats(allMovies);
    } catch (err) {
        console.error("❌ Load movies error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tải danh sách phim!", "error");
    }
}

// 🔹 Chuyển phim sang trạng thái Ended
window.deleteMovie = async function (id) {
    Swal.fire({
        title: "Xác nhận",
        text: "Chuyển phim này sang trạng thái 'Ended'?",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Đồng ý",
        cancelButtonText: "Hủy"
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const res = await movieApi.getById(id);
                const movie = res?.result || res;
                movie.status = "Ended";

                const formData = new FormData();
                formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));

                await movieApi.update(id, formData);

                Swal.fire("Thành công", "Phim đã được chuyển sang 'Ended'", "success")
                    .then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể cập nhật phim!", "error");
            }
        }
    });
};

// 🔹 Khôi phục phim về trạng thái chiếu
window.restoreMovie = async function (id) {
    Swal.fire({
        title: "Khôi phục phim",
        text: "Chọn trạng thái mới cho phim này:",
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
                const res = await movieApi.getById(id);
                const movie = res?.result || res;
                movie.status = newStatus;

                const formData = new FormData();
                formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));

                await movieApi.update(id, formData);

                Swal.fire("Thành công", `Đã khôi phục sang '${newStatus}'`, "success")
                    .then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể khôi phục phim!", "error");
            }
        }
    });
};

// 🔹 Hiển thị trailer YouTube (đã FIX lỗi X-Frame-Options)
window.showTrailer = function (url) {
    if (!url || !url.includes("youtu")) {
        Swal.fire("Lỗi", "Trailer không hợp lệ hoặc chưa được thêm!", "error");
        return;
    }

    // 🎯 Lấy video ID chính xác (dạng watch?v= hoặc youtu.be/)
    const match = url.match(/(?:v=|youtu\.be\/)([^#&?]*)/);
    const videoId = match ? match[1] : null;

    if (!videoId) {
        Swal.fire("Lỗi", "Không thể nhận dạng video YouTube hợp lệ!", "error");
        return;
    }

    // 🎬 Tạo link embed hợp lệ
    const embedUrl = `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=0&controls=0&modestbranding=1&rel=0&loop=1&playlist=${videoId}`;

    Swal.fire({
        html: `
            <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;border-radius:12px;">
                <iframe 
                    src="${embedUrl}" 
                    frameborder="0" 
                    allow="autoplay; encrypted-media" 
                    allowfullscreen 
                    style="position:absolute;top:0;left:0;width:100%;height:100%;"></iframe>
            </div>
        `,
        width: 900,
        background: "transparent",
        showConfirmButton: false,
        showCloseButton: true,
    });
};

// 🔹 Nút sửa phim
window.editMovie = function (id) {
    if (!id || id === "undefined" || isNaN(Number(id))) {
        Swal.fire("Lỗi", "Không tìm thấy ID phim hợp lệ để chỉnh sửa!", "error");
        return;
    }
    window.location.href = `updateMovie.html?id=${id}`;
};

// 🔹 Cập nhật thống kê và chart
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
            labels: ["Đang chiếu", "Sắp chiếu", "Đã kết thúc"],
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

// 🔹 Load khởi đầu
loadMovies();
