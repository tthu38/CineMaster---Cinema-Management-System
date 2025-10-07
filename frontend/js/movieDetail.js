// const API_BASE = "http://localhost:8080/api/v1";
// const params = new URLSearchParams(window.location.search);
// const movieId = params.get("id");
//
// // 🟢 Lấy token người dùng (nếu có)
// const token = localStorage.getItem("accessToken");
// let currentUserId = null;
//
// // ===================== Lấy accountID từ token =====================
// function parseJwt(token) {
//     try {
//         return JSON.parse(atob(token.split('.')[1]));
//     } catch (e) {
//         return null;
//     }
// }
//
// if (token) {
//     const decoded = parseJwt(token);
//     if (decoded && decoded.accountId) {
//         currentUserId = decoded.accountId;
//     }
// }
//
// // ===================== DOM ELEMENTS =====================
// const title = document.getElementById("title");
// const genreText = document.getElementById("genre-text");
// const descriptions = document.getElementById("descriptions");
// const director = document.getElementById("director");
// const releaseDate = document.getElementById("releaseDate");
// const languages = document.getElementById("languages");
// const poster = document.getElementById("poster");
// const heroPoster = document.getElementById("hero-poster");
// const casts = document.getElementById("casts");
// const ageRestrictionText = document.getElementById("ageRestrictionText");
// const btnTrailer = document.getElementById("btn-trailer");
//
// // Badges
// const ageRestrictionBadge = document.getElementById("ageRestrictionBadge");
// const statusBadge = document.getElementById("statusBadge");
// const durationBadge = document.getElementById("durationBadge");
// const countryBadge = document.getElementById("countryBadge");
//
// const feedbackList = document.getElementById("feedback-list");
// const avgRating = document.getElementById("avg-rating");
// const feedbackCount = document.getElementById("feedback-count");
// const btnSubmit = document.getElementById("btn-submit");
// const btnBack = document.getElementById("btn-back");
//
// // ===================== CẬP NHẬT BADGES =====================
// function updateBadges(movie) {
//     ageRestrictionBadge.textContent = movie.ageRestriction || "N/A";
//     ageRestrictionText.textContent = movie.ageRestriction || "N/A";
//     durationBadge.innerHTML = `<i class="far fa-clock"></i> ${movie.duration} MINS`;
//     countryBadge.innerHTML = `<i class="fa-solid fa-globe"></i> ${movie.country || 'N/A'}`;
//
//     statusBadge.textContent = movie.status?.toUpperCase() || "UNKNOWN";
//     statusBadge.classList.remove('badge-status-now', 'badge-status-soon', 'badge-status-ended');
//
//     if (movie.status === 'Now Showing') {
//         statusBadge.classList.add('badge-status-now');
//     } else if (movie.status === 'Coming Soon') {
//         statusBadge.classList.add('badge-status-soon');
//     } else {
//         statusBadge.classList.add('badge-status-ended');
//     }
// }
//
// // ===================== LOAD MOVIE =====================
// async function loadMovie() {
//     try {
//         const res = await fetch(`${API_BASE}/movies/${movieId}`);
//         const data = await res.json();
//
//         if (data.code === 1000) {
//             const m = data.result;
//
//             // ✅ Sửa đúng các key field
//             title.textContent = `TITLE: ${m.title.toUpperCase()}`;
//             genreText.innerHTML = `<i class="fa-solid fa-tags"></i> **Thể loại:** ${m.genre}`;
//             descriptions.textContent = m.description || "Không có mô tả chi tiết.";
//             director.textContent = m.director || "N/A";
//             releaseDate.textContent = new Date(m.releaseDate).toLocaleDateString('vi-VN');
//             languages.textContent = m.language || "N/A";
//             casts.textContent = m.cast || "Chưa cập nhật diễn viên.";
//             btnTrailer.href = m.trailerUrl || '#';
//             btnTrailer.target = m.trailerUrl ? '_blank' : '_self';
//
//             // Ảnh
//             const posterUrl = m.posterUrl || "../image/default_movie.jpg";
//             poster.src = posterUrl;
//             heroPoster.src = posterUrl;
//
//             updateBadges(m);
//         } else {
//             title.textContent = "Lỗi: Không tìm thấy phim";
//             descriptions.textContent = data.message;
//         }
//     } catch (error) {
//         console.error("Lỗi tải phim:", error);
//         title.textContent = "Lỗi kết nối API";
//     }
// }
//
// // ===================== LOAD FEEDBACKS =====================
// async function loadFeedbacks() {
//     const res = await fetch(`${API_BASE}/feedback/movie/${movieId}`);
//     const data = await res.json();
//     feedbackList.innerHTML = "";
//
//     if (data.code === 1000 && data.result.length > 0) {
//         const feedbacks = data.result;
//         const total = feedbacks.reduce((sum, f) => sum + f.rating, 0);
//         avgRating.textContent = `⭐ ${(total / feedbacks.length).toFixed(1)}`;
//         feedbackCount.textContent = `(${feedbacks.length} đánh giá đã được ghi nhận)`;
//
//         feedbacks.forEach(f => {
//             const div = document.createElement("div");
//             div.className = "feedback-item p-3 mb-3";
//
//             let actions = "";
//             if (currentUserId && f.accountId === currentUserId) {
//                 actions = `
//                     <button class="btn btn-sm btn-warning btn-edit me-2"
//                             data-id="${f.id}" data-rating="${f.rating}" data-comment="${f.comment}">
//                         <i class="fa fa-edit"></i> Sửa
//                     </button>
//                     <button class="btn btn-sm btn-danger btn-delete" data-id="${f.id}">
//                         <i class="fa fa-trash"></i> Xoá
//                     </button>`;
//             }
//
//             div.innerHTML = `
//                 <div class="d-flex justify-content-between align-items-center mb-1">
//                     <div>
//                         <span class="name">${f.accountName}</span>
//                         <span class="date ms-3 text-muted"><i class="fa-regular fa-clock"></i>
//                             ${new Date(f.createdAt).toLocaleString()}</span>
//                     </div>
//                     <div class="stars">${"⭐".repeat(f.rating)}</div>
//                 </div>
//                 <p class="mt-2 mb-0 text-light">${f.comment}</p>
//                 <div class="mt-2">${actions}</div>
//             `;
//             feedbackList.appendChild(div);
//         });
//
//         bindFeedbackActions();
//     } else {
//         avgRating.textContent = "⭐ 0.0";
//         feedbackCount.textContent = "(0 đánh giá đã được ghi nhận)";
//         feedbackList.innerHTML = `<p class="text-center text-muted fst-italic">CHƯA CÓ DỮ LIỆU ĐÁNH GIÁ.</p>`;
//     }
// }
//
// // ===================== GỬI FEEDBACK =====================
// btnSubmit.addEventListener("click", async () => {
//     if (!token) {
//         Swal.fire({
//             icon: "warning",
//             title: "Bạn cần đăng nhập để đánh giá!",
//             confirmButtonText: "Đăng nhập ngay",
//         }).then(() => (window.location.href = "../user/login.html"));
//         return;
//     }
//
//     const rating = parseInt(document.getElementById("rating").value);
//     const comment = document.getElementById("comment").value.trim();
//     if (!comment) {
//         Swal.fire("Thông báo", "Vui lòng nhập nội dung!", "warning");
//         return;
//     }
//
//     const payload = { rating, comment };
//
//     const res = await fetch(`${API_BASE}/feedback/movie/${movieId}`, {
//         method: "POST",
//         headers: {
//             "Content-Type": "application/json",
//             Authorization: `Bearer ${token}`,
//         },
//         body: JSON.stringify(payload),
//     });
//
//     const data = await res.json();
//     if (data.code === 1000) {
//         Swal.fire("Cảm ơn!", "Đánh giá đã được gửi!", "success");
//         document.getElementById("comment").value = "";
//         loadFeedbacks();
//     } else Swal.fire("Lỗi", data.message, "error");
// });
//
// // ===================== EDIT & DELETE =====================
// function bindFeedbackActions() {
//     document.querySelectorAll(".btn-delete").forEach(btn => {
//         btn.onclick = async () => {
//             const id = btn.dataset.id;
//             if (!token) {
//                 Swal.fire("Thông báo", "Bạn cần đăng nhập để xóa đánh giá!", "warning");
//                 return;
//             }
//
//             const confirm = await Swal.fire({
//                 title: "Xác nhận xóa?",
//                 icon: "warning",
//                 showCancelButton: true,
//                 confirmButtonText: "Xóa"
//             });
//             if (!confirm.isConfirmed) return;
//
//             const res = await fetch(`${API_BASE}/feedback/${id}`, {
//                 method: "DELETE",
//                 headers: { Authorization: `Bearer ${token}` }
//             });
//             const data = await res.json();
//             if (data.code === 1000) {
//                 Swal.fire("Đã xóa!", "", "success");
//                 loadFeedbacks();
//             } else Swal.fire("Lỗi", data.message, "error");
//         };
//     });
//
//     document.querySelectorAll(".btn-edit").forEach(btn => {
//         btn.onclick = async () => {
//             const id = btn.dataset.id;
//             const oldRating = btn.dataset.rating;
//             const oldComment = btn.dataset.comment;
//
//             if (!token) {
//                 Swal.fire("Thông báo", "Bạn cần đăng nhập để chỉnh sửa!", "warning");
//                 return;
//             }
//
//             const { value: formValues } = await Swal.fire({
//                 title: "Chỉnh sửa đánh giá",
//                 html: `
//                     <select id="swal-rating" class="form-select mb-2">
//                         ${[5,4,3,2,1].map(r =>
//                     `<option value="${r}" ${r==oldRating?"selected":""}>⭐ ${r}</option>`
//                 ).join('')}
//                     </select>
//                     <textarea id="swal-comment" class="form-control" rows="3">${oldComment}</textarea>`,
//                 preConfirm: () => ({
//                     rating: parseInt(document.getElementById("swal-rating").value),
//                     comment: document.getElementById("swal-comment").value.trim()
//                 })
//             });
//             if (!formValues) return;
//
//             const res = await fetch(`${API_BASE}/feedback/${id}`, {
//                 method: "PUT",
//                 headers: {
//                     "Content-Type": "application/json",
//                     Authorization: `Bearer ${token}`
//                 },
//                 body: JSON.stringify(formValues)
//             });
//             const data = await res.json();
//             if (data.code === 1000) {
//                 Swal.fire("Đã cập nhật!", "", "success");
//                 loadFeedbacks();
//             } else Swal.fire("Lỗi", data.message, "error");
//         };
//     });
// }
//
// // ===================== BACK BUTTON =====================
// btnBack.onclick = () => window.location.href = "listMovieCus.html";
//
// // ===================== INIT =====================
// loadMovie();
// loadFeedbacks();
const API_BASE = "http://localhost:8080/api/v1";
const params = new URLSearchParams(window.location.search);
const movieId = params.get("id");

// 🟢 Lấy token người dùng (nếu có)
const token = localStorage.getItem("accessToken");
let currentUserId = null;

// ===================== Lấy accountID từ token =====================
function parseJwt(token) {
    try {
        return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
        return null;
    }
}

if (token) {
    const decoded = parseJwt(token);
    if (decoded && decoded.accountId) {
        currentUserId = decoded.accountId;
    }
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

// Trailer
const trailerWrapper = document.getElementById("trailer-wrapper");

// Badges
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
    countryBadge.innerHTML = `<i class="fa-solid fa-globe"></i> ${movie.country || 'N/A'}`;

    statusBadge.textContent = movie.status?.toUpperCase() || "UNKNOWN";
    statusBadge.classList.remove('badge-status-now', 'badge-status-soon', 'badge-status-ended');

    if (movie.status === 'Now Showing') {
        statusBadge.classList.add('badge-status-now');
    } else if (movie.status === 'Coming Soon') {
        statusBadge.classList.add('badge-status-soon');
    } else {
        statusBadge.classList.add('badge-status-ended');
    }
}

// ===================== LOAD MOVIE =====================
async function loadMovie() {
    try {
        const res = await fetch(`${API_BASE}/movies/${movieId}`);
        const data = await res.json();

        if (data.code === 1000) {
            const m = data.result;

            // ✅ Các thông tin chính
            title.textContent = `TITLE: ${m.title.toUpperCase()}`;
            genreText.innerHTML = `<i class="fa-solid fa-tags"></i> **Thể loại:** ${m.genre}`;
            descriptions.textContent = m.description || "Không có mô tả chi tiết.";
            director.textContent = m.director || "N/A";
            releaseDate.textContent = new Date(m.releaseDate).toLocaleDateString('vi-VN');
            languages.textContent = m.language || "N/A";
            casts.textContent = m.cast || "Chưa cập nhật diễn viên.";
            btnTrailer.href = m.trailerUrl || '#';
            btnTrailer.target = m.trailerUrl ? '_blank' : '_self';

            // Poster
            const posterUrl = m.posterUrl || "../image/default_movie.jpg";
            poster.src = posterUrl;
            heroPoster.src = posterUrl;

            updateBadges(m);

            // 🎥 Hiển thị trailer auto-play (ẩn controls, không click)
            if (m.trailerUrl) {
                const embedUrl = getEmbedUrl(m.trailerUrl);
                trailerWrapper.innerHTML = `
                    <iframe id="trailer-frame"
                        src="${embedUrl}"
                        class="trailer-embed"
                        allow="autoplay; encrypted-media"
                        allowfullscreen
                        style="width:100%;aspect-ratio:16/9;border:none;border-radius:10px;pointer-events:none;">
                    </iframe>
                `;
            } else {
                trailerWrapper.innerHTML = `<p class="text-center text-muted fst-italic mt-3">Không có trailer khả dụng.</p>`;
            }

        } else {
            title.textContent = "Lỗi: Không tìm thấy phim";
            descriptions.textContent = data.message;
        }
    } catch (error) {
        console.error("Lỗi tải phim:", error);
        title.textContent = "Lỗi kết nối API";
    }
}

// ===================== LOAD FEEDBACKS =====================
async function loadFeedbacks() {
    const res = await fetch(`${API_BASE}/feedback/movie/${movieId}`);
    const data = await res.json();
    feedbackList.innerHTML = "";

    if (data.code === 1000 && data.result.length > 0) {
        const feedbacks = data.result;
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
                            ${new Date(f.createdAt).toLocaleString()}</span>
                    </div>
                    <div class="stars">${"⭐".repeat(f.rating)}</div>
                </div>
                <p class="mt-2 mb-0 text-light">${f.comment}</p>
                <div class="mt-2">${actions}</div>
            `;
            feedbackList.appendChild(div);
        });

        bindFeedbackActions();
    } else {
        avgRating.textContent = "⭐ 0.0";
        feedbackCount.textContent = "(0 đánh giá đã được ghi nhận)";
        feedbackList.innerHTML = `<p class="text-center text-muted fst-italic">CHƯA CÓ DỮ LIỆU ĐÁNH GIÁ.</p>`;
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
    if (!comment) {
        Swal.fire("Thông báo", "Vui lòng nhập nội dung!", "warning");
        return;
    }

    const payload = { rating, comment };

    const res = await fetch(`${API_BASE}/feedback/movie/${movieId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
    });

    const data = await res.json();
    if (data.code === 1000) {
        Swal.fire("Cảm ơn!", "Đánh giá đã được gửi!", "success");
        document.getElementById("comment").value = "";
        loadFeedbacks();
    } else Swal.fire("Lỗi", data.message, "error");
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
                confirmButtonText: "Xóa"
            });
            if (!confirm.isConfirmed) return;

            const res = await fetch(`${API_BASE}/feedback/${id}`, {
                method: "DELETE",
                headers: { Authorization: `Bearer ${token}` }
            });
            const data = await res.json();
            if (data.code === 1000) {
                Swal.fire("Đã xóa!", "", "success");
                loadFeedbacks();
            } else Swal.fire("Lỗi", data.message, "error");
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
                        ${[5,4,3,2,1].map(r => `<option value="${r}" ${r==oldRating?"selected":""}>⭐ ${r}</option>`).join('')}
                    </select>
                    <textarea id="swal-comment" class="form-control" rows="3">${oldComment}</textarea>`,
                preConfirm: () => ({
                    rating: parseInt(document.getElementById("swal-rating").value),
                    comment: document.getElementById("swal-comment").value.trim()
                })
            });

            if (!formValues) return;

            const res = await fetch(`${API_BASE}/feedback/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify(formValues)
            });
            const data = await res.json();
            if (data.code === 1000) {
                Swal.fire("Đã cập nhật!", "", "success");
                loadFeedbacks();
            } else Swal.fire("Lỗi", data.message, "error");
        };
    });
}

// ===================== BACK BUTTON =====================
btnBack.onclick = () => window.location.href = "listMovieCus.html";

// ===================== INIT =====================
loadMovie();
loadFeedbacks();
