import { movieApi } from "./api/movieApi.js";

const tbody = document.getElementById("movies-body");
const pageNumbers = document.getElementById("page-numbers");
let movieChart = null;
let allMovies = [];
let currentPage = 1;
const pageSize = 10;

/* ===================== SORT MOVIES ===================== */
function sortMovies(movies) {
    const order = { "Now Showing": 1, "Coming Soon": 2, "Ended": 3 };
    return movies.sort((a, b) => (order[a.status] || 99) - (order[b.status] || 99));
}

/* ===================== RENDER MOVIES ===================== */
function renderMovies(movies, page = 1) {
    tbody.innerHTML = "";
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    const pageMovies = movies.slice(start, end);

    if (pageMovies.length === 0) {
        tbody.innerHTML = `
      <tr><td colspan="8" class="text-muted py-3">Không có phim nào để hiển thị.</td></tr>`;
        return;
    }

    pageMovies.forEach((m) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td><img src="${m.posterUrl || ""}" class="movie-poster"/></td>
      <td title="${m.title}" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
          ${m.title}
      </td>
      <td>${m.genre || "-"}</td>
      <td>${m.duration || 0} phút</td>
      <td>${m.releaseDate || "-"}</td>
      <td>${m.director || "-"}</td>
      <td>${m.status || "-"}</td>
      <td style="min-width:180px;">
        <div class="btn-group-action">
          ${m.status !== "Ended"
            ? `
              <button class="btn btn-sm btn-primary" onclick="editMovie(${m.movieID})">Sửa</button>
              <button class="btn btn-sm btn-danger" onclick="deleteMovie(${m.movieID})">Đóng</button>
            `
            : `
              <button class="btn btn-sm btn-success" onclick="restoreMovie(${m.movieID})">Mở lại</button>
            `}
          <button class="btn btn-sm btn-warning" onclick="showTrailer('${m.trailerUrl || ""}')">Trailer</button>
        </div>
      </td>
    `;
        tbody.appendChild(tr);
    });

    renderPagination(movies);
}

function renderPagination(movies) {
    const totalPages = Math.ceil(movies.length / pageSize);
    const pagination = document.getElementById("page-numbers");
    pagination.innerHTML = "";

    if (totalPages <= 1) return;

    const ul = document.createElement("ul");
    ul.className = "pagination pagination-sm";

    const createItem = (label, page, disabled = false, active = false) => {
        const li = document.createElement("li");
        li.className = `page-item ${disabled ? "disabled" : ""} ${active ? "active" : ""}`;
        const a = document.createElement("a");
        a.className = "page-link";
        a.href = "#";
        a.textContent = label;
        if (!disabled) {
            a.addEventListener("click", (e) => {
                e.preventDefault();
                goToPage(page);
            });
        }
        li.appendChild(a);
        return li;
    };
    ul.appendChild(createItem("«", currentPage - 1, currentPage === 1));
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, currentPage + 2);
    for (let i = start; i <= end; i++) {
        ul.appendChild(createItem(i, i, false, i === currentPage));
    }
    ul.appendChild(createItem("»", currentPage + 1, currentPage === totalPages));

    pagination.appendChild(ul);
}

function goToPage(page) {
    const totalPages = Math.ceil(allMovies.length / pageSize);
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    renderMovies(allMovies, currentPage);
}

/* ===================== LOAD MOVIES ===================== */
async function loadMovies() {
    try {
        const res = await movieApi.getAll();
        allMovies = res?.result || res;
        allMovies = sortMovies(allMovies);
        currentPage = 1;
        renderMovies(allMovies, currentPage);
        updateStats(allMovies);
    } catch (err) {
        console.error("Load movies error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tải danh sách phim!", "error");
    }
}

/* ===================== DELETE (SET ENDED) ===================== */
window.deleteMovie = async function (id) {
    Swal.fire({
        title: "Xác nhận",
        text: "Chuyển phim này sang trạng thái 'Ended'?",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Đồng ý",
        cancelButtonText: "Hủy",
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const res = await movieApi.getById(id);
                const movie = res?.result || res;
                movie.status = "Ended";

                const formData = new FormData();
                formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));

                await movieApi.update(id, formData);
                Swal.fire("Thành công", "Phim đã được chuyển sang 'Ended'", "success").then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể cập nhật phim!", "error");
            }
        }
    });
};

/* ===================== RESTORE MOVIE ===================== */
window.restoreMovie = async function (id) {
    Swal.fire({
        title: "Khôi phục phim",
        text: "Chọn trạng thái mới cho phim này:",
        icon: "question",
        showDenyButton: true,
        showCancelButton: true,
        confirmButtonText: "Now Showing",
        denyButtonText: "Coming Soon",
        cancelButtonText: "Hủy",
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
                Swal.fire("Thành công", `Đã khôi phục sang '${newStatus}'`, "success").then(() => loadMovies());
            } catch (err) {
                Swal.fire("Lỗi", err.message || "Không thể khôi phục phim!", "error");
            }
        }
    });
};

/* ===================== SHOW TRAILER ===================== */
window.showTrailer = function (url) {
    if (!url || url.trim() === "") {
        Swal.fire("Thông báo", "Phim này chưa có trailer!", "info");
        return;
    }

    let htmlContent = "";
    if (url.includes("youtu")) {
        const match = url.match(/(?:v=|youtu\.be\/)([^#&?]*)/);
        const videoId = match ? match[1] : null;
        if (!videoId) {
            Swal.fire("Lỗi", "Không thể nhận dạng video YouTube hợp lệ!", "error");
            return;
        }
        const embedUrl = `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=0&controls=1&modestbranding=1&rel=0&loop=1&playlist=${videoId}`;
        htmlContent = `
      <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;border-radius:12px;">
        <iframe src="${embedUrl}" frameborder="0"
          allow="autoplay; encrypted-media"
          allowfullscreen
          style="position:absolute;top:0;left:0;width:100%;height:100%;"></iframe>
      </div>`;
    } else if (url.includes("cloudinary.com")) {
        htmlContent = `
      <video src="${url}" controls autoplay
        style="width:100%;border-radius:12px;outline:none;box-shadow:0 0 20px rgba(34,193,255,.3)">
        Trình duyệt của bạn không hỗ trợ video.
      </video>`;
    } else {
        Swal.fire("Lỗi", "Trailer không hợp lệ!", "error");
        return;
    }

    Swal.fire({
        html: htmlContent,
        width: 900,
        background: "rgba(10,20,40,0.95)",
        showConfirmButton: false,
        showCloseButton: true,
    });
};

/* ===================== EDIT MOVIE ===================== */
window.editMovie = function (id) {
    if (!id || id === "undefined" || isNaN(Number(id))) {
        Swal.fire("Lỗi", "Không tìm thấy ID phim hợp lệ để chỉnh sửa!", "error");
        return;
    }
    window.location.href = `updateMovie.html?id=${id}`;
};

/* ===================== UPDATE STATS & CHART ===================== */
function updateStats(movies) {
    const total = movies.length;
    const nowShowing = movies.filter((m) => m.status === "Now Showing").length;
    const comingSoon = movies.filter((m) => m.status === "Coming Soon").length;

    document.getElementById("total-movies").textContent = total;
    document.getElementById("current-movies").textContent = nowShowing;
    document.getElementById("upcoming-movies").textContent = comingSoon;

    const ctx = document.getElementById("movieStatusChart").getContext("2d");
    if (movieChart) movieChart.destroy();

    movieChart = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Đang chiếu", "Sắp chiếu", "Đã kết thúc"],
            datasets: [
                {
                    data: [nowShowing, comingSoon, total - nowShowing - comingSoon],
                    backgroundColor: ["#0aa3ff", "#22c1ff", "#e50914"],
                },
            ],
        },
        options: {
            plugins: { legend: { labels: { color: "#fff" } } },
        },
    });
}

/* ===================== INIT ===================== */
loadMovies();
