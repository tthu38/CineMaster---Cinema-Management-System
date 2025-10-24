import { movieApi } from "./api/movieApi.js";
import { feedbackApi } from "./api/feedbackApi.js";
import { getValidToken } from "./api/config.js";
import { branchApi } from "./api/branchApi.js";




const params = new URLSearchParams(window.location.search);
const movieId = params.get("id");


// 🟢 Lấy token người dùng (nếu có)
const token = getValidToken();
let currentUserId = null;


// ===================== Giải mã JWT =====================
function parseJwt(token) {
    try {
        return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
        return null;
    }
}
if (token) {
    const decoded = parseJwt(token);
    if (decoded && decoded.accountId) currentUserId = decoded.accountId;
}


// ===================== DOM ELEMENTS =====================
const title = document.getElementById("title");
const genreText = document.getElementById("genre-text");
const descriptions = document.getElementById("descriptions");
const director = document.getElementById("director");
const releaseDate = document.getElementById("releaseDate");
const languages = document.getElementById("languages");
const poster = document.getElementById("poster");
const heroPoster = document.getElementById("hero-poster");
const casts = document.getElementById("casts");
const ageRestrictionText = document.getElementById("ageRestrictionText");
const btnTrailer = document.getElementById("btn-trailer");
const btnBooking = document.getElementById("btn-booking");


const trailerWrapper = document.getElementById("trailer-wrapper");


const ageRestrictionBadge = document.getElementById("ageRestrictionBadge");
const statusBadge = document.getElementById("statusBadge");
const durationBadge = document.getElementById("durationBadge");
const countryBadge = document.getElementById("countryBadge");


const feedbackList = document.getElementById("feedback-list");
const avgRating = document.getElementById("avg-rating");
const feedbackCount = document.getElementById("feedback-count");
const btnSubmit = document.getElementById("btn-submit");
const btnBack = document.getElementById("btn-back");


// ===================== Convert YouTube URL sang embed =====================
function getEmbedUrl(url) {
    const reg = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=)([^#&?]*).*/;
    const m = url.match(reg);
    if (m && m[2].length === 11) {
        const id = m[2];
        return `https://www.youtube.com/embed/${id}?autoplay=1&mute=0&controls=0&modestbranding=1&rel=0&loop=1&playlist=${id}`;
    }
    return "";
}


// ===================== CẬP NHẬT BADGES =====================
function updateBadges(movie) {
    ageRestrictionBadge.textContent = movie.ageRestriction || "N/A";
    ageRestrictionText.textContent = movie.ageRestriction || "N/A";
    durationBadge.innerHTML = `<i class="far fa-clock"></i> ${movie.duration} MINS`;
    countryBadge.innerHTML = `<i class="fa-solid fa-globe"></i> ${movie.country || "N/A"}`;


    statusBadge.textContent = movie.status?.toUpperCase() || "UNKNOWN";
    statusBadge.classList.remove("badge-status-now", "badge-status-soon", "badge-status-ended");
    if (movie.status === "Now Showing") statusBadge.classList.add("badge-status-now");
    else if (movie.status === "Coming Soon") statusBadge.classList.add("badge-status-soon");
    else statusBadge.classList.add("badge-status-ended");
}


// ===================== LOAD MOVIE =====================
async function loadMovie() {
    try {
        const m = await movieApi.getById(movieId);


        title.textContent = `TITLE: ${m.title.toUpperCase()}`;
        genreText.innerHTML = `<i class="fa-solid fa-tags"></i> Thể loại: ${m.genre}`;
        descriptions.textContent = m.description || "Không có mô tả chi tiết.";
        director.textContent = m.director || "N/A";
        releaseDate.textContent = new Date(m.releaseDate).toLocaleDateString("vi-VN");
        languages.textContent = m.language || "N/A";
        casts.textContent = m.cast || "Chưa cập nhật diễn viên.";
        btnTrailer.href = m.trailerUrl || "#";
        btnTrailer.target = m.trailerUrl ? "_blank" : "_self";


        const posterUrl = m.posterUrl || "../image/default_movie.jpg";
        poster.src = posterUrl;
        heroPoster.src = posterUrl;
        updateBadges(m);


        if (m.trailerUrl) {
            const embedUrl = getEmbedUrl(m.trailerUrl);
            trailerWrapper.innerHTML = `
               <iframe
                   src="${embedUrl}"
                   class="trailer-embed"
                   allow="autoplay; encrypted-media"
                   allowfullscreen
                   style="width:100%;aspect-ratio:16/9;border:none;border-radius:10px;pointer-events:none;">
               </iframe>`;
        } else {
            trailerWrapper.innerHTML = `<p class="text-center text-muted fst-italic mt-3">Không có trailer khả dụng.</p>`;
        }
    } catch (err) {
        console.error("Lỗi tải phim:", err);
        title.textContent = "Không tìm thấy phim hoặc lỗi API.";
    }
}


// ===================== LOAD FEEDBACKS =====================
async function loadFeedbacks() {
    try {
        const feedbacks = await feedbackApi.getByMovie(movieId);
        feedbackList.innerHTML = "";


        if (!feedbacks || feedbacks.length === 0) {
            avgRating.textContent = "⭐ 0.0";
            feedbackCount.textContent = "(0 đánh giá)";
            feedbackList.innerHTML = `<p class="text-center text-muted fst-italic">CHƯA CÓ DỮ LIỆU ĐÁNH GIÁ.</p>`;
            return;
        }


        const total = feedbacks.reduce((sum, f) => sum + f.rating, 0);
        avgRating.textContent = `⭐ ${(total / feedbacks.length).toFixed(1)}`;
        feedbackCount.textContent = `(${feedbacks.length} đánh giá đã được ghi nhận)`;


        feedbacks.forEach(f => {
            const div = document.createElement("div");
            div.className = "feedback-item p-3 mb-3";


            let actions = "";
            if (currentUserId && f.accountId === currentUserId) {
                actions = `
                   <button class="btn btn-sm btn-warning btn-edit me-2"
                           data-id="${f.id}" data-rating="${f.rating}" data-comment="${f.comment}">
                       <i class="fa fa-edit"></i> Sửa
                   </button>
                   <button class="btn btn-sm btn-danger btn-delete" data-id="${f.id}">
                       <i class="fa fa-trash"></i> Xoá
                   </button>`;
            }


            div.innerHTML = `
               <div class="d-flex justify-content-between align-items-center mb-1">
                   <div>
                       <span class="name">${f.accountName}</span>
                       <span class="date ms-3 text-muted"><i class="fa-regular fa-clock"></i>
                           ${new Date(f.createdAt).toLocaleString("vi-VN")}</span>
                   </div>
                   <div class="stars">${"⭐".repeat(f.rating)}</div>
               </div>
               <p class="mt-2 mb-0 text-light">${f.comment}</p>
               <div class="mt-2">${actions}</div>
           `;
            feedbackList.appendChild(div);
        });


        bindFeedbackActions();
    } catch (err) {
        console.error("Lỗi tải feedback:", err);
        feedbackList.innerHTML = `<p class="text-center text-muted fst-italic">Không thể tải đánh giá.</p>`;
    }
}


// ===================== GỬI FEEDBACK =====================
btnSubmit.addEventListener("click", async () => {
    if (!token) {
        Swal.fire({
            icon: "warning",
            title: "Bạn cần đăng nhập để đánh giá!",
            confirmButtonText: "Đăng nhập ngay",
        }).then(() => (window.location.href = "../user/login.html"));
        return;
    }


    const rating = parseInt(document.getElementById("rating").value);
    const comment = document.getElementById("comment").value.trim();
    if (!comment) return Swal.fire("Thông báo", "Vui lòng nhập nội dung!", "warning");


    try {
        await feedbackApi.create(movieId, { rating, comment });
        Swal.fire("Cảm ơn!", "Đánh giá đã được gửi!", "success");
        document.getElementById("comment").value = "";
        loadFeedbacks();
    } catch (err) {
        Swal.fire("Lỗi", err.message, "error");
    }
});


// ===================== EDIT & DELETE =====================
function bindFeedbackActions() {
    document.querySelectorAll(".btn-delete").forEach(btn => {
        btn.onclick = async () => {
            const id = btn.dataset.id;
            const confirm = await Swal.fire({
                title: "Xác nhận xóa?",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Xóa",
            });
            if (!confirm.isConfirmed) return;
            try {
                await feedbackApi.delete(id);
                Swal.fire("Đã xóa!", "", "success");
                loadFeedbacks();
            } catch (err) {
                Swal.fire("Lỗi", err.message, "error");
            }
        };
    });


    document.querySelectorAll(".btn-edit").forEach(btn => {
        btn.onclick = async () => {
            const id = btn.dataset.id;
            const oldRating = btn.dataset.rating;
            const oldComment = btn.dataset.comment;


            const { value: formValues } = await Swal.fire({
                title: "Chỉnh sửa đánh giá",
                html: `
                   <select id="swal-rating" class="form-select mb-2">
                       ${[5,4,3,2,1].map(r =>
                    `<option value="${r}" ${r==oldRating?"selected":""}>⭐ ${r}</option>`
                ).join("")}
                   </select>
                   <textarea id="swal-comment" class="form-control" rows="3">${oldComment}</textarea>`,
                preConfirm: () => ({
                    rating: parseInt(document.getElementById("swal-rating").value),
                    comment: document.getElementById("swal-comment").value.trim(),
                }),
            });
            if (!formValues) return;


            try {
                await feedbackApi.update(id, formValues);
                Swal.fire("Đã cập nhật!", "", "success");
                loadFeedbacks();
            } catch (err) {
                Swal.fire("Lỗi", err.message, "error");
            }
        };
    });
}


// ===================== BACK BUTTON =====================
btnBack.onclick = () => (window.location.href = "listMovieCus.html");
// ===================== BOOKING BUTTON =====================
btnBooking.addEventListener("click", async (e) => {
    e.preventDefault();


    if (!movieId) {
        Swal.fire("Lỗi", "Không tìm thấy mã phim để đặt vé.", "error");
        return;
    }


    try {
        const branches = await branchApi.getBranchesByMovie(movieId);


        if (!branches || branches.length === 0) {
            Swal.fire({
                icon: "info",
                title: "Hiện chưa có rạp nào chiếu phim này!",
                text: "Vui lòng quay lại sau.",
                confirmButtonText: "Đóng",
            });
            return;
        }


        // ✅ Hiển thị popup chọn chi nhánh
        const { value: selectedBranch } = await Swal.fire({
            title: "Chọn chi nhánh để đặt vé",
            input: "select",
            inputOptions: branches.reduce((acc, b) => {
                acc[b.branchId] = `${b.branchName} - ${b.address}`;
                return acc;
            }, {}),
            inputPlaceholder: "Chọn một chi nhánh...",
            showCancelButton: true,
            confirmButtonText: "Xem lịch chiếu",
            cancelButtonText: "Hủy",
            inputValidator: (value) => {
                if (!value) return "Vui lòng chọn chi nhánh!";
            },
        });


        if (selectedBranch) {
            // ✅ Lưu movieId để showtimes page biết đang xem phim nào
            localStorage.setItem("selectedMovieId", movieId);
            localStorage.setItem("selectedBranchId", selectedBranch);


            // ✅ Điều hướng tới lịch chiếu của chi nhánh đó
            window.location.href = `../user/showtimes-calendar.html?branchId=${selectedBranch}&movieId=${movieId}`;
        }
    } catch (err) {
        console.error("Lỗi khi tải chi nhánh:", err);
        Swal.fire("Lỗi", "Không thể tải danh sách chi nhánh!", "error");
    }
});




// ===================== INIT =====================
loadMovie();
loadFeedbacks();



