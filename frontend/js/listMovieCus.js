import { movieApi } from "./api/movieApi.js";

const movieList = document.getElementById("movie-list");
const btnNow = document.getElementById("btn-now-showing");
const btnSoon = document.getElementById("btn-coming-soon");
const btnAll = document.getElementById("btn-all");

// LẤY CÁC BIẾN DOM MỚI (TỪ HTML ĐÃ SỬA)
const titleSearchInput = document.getElementById("title-search-input");
const searchByTitleBtn = document.getElementById("search-by-title-btn");

const genreFilter = document.getElementById("genre-filter");
const languageFilter = document.getElementById("language-filter");
const directorFilter = document.getElementById("director-filter");
const castFilter = document.getElementById("cast-filter");
const applyFilterBtn = document.getElementById("apply-filter-btn");

let currentFilter = "Now Showing";
let allMovies = []; // Vẫn dùng để cache kết quả gần nhất

// ========================== LOAD & FILTER MOVIES (SỬ DỤNG API SEARCH NÂNG CAO) ==========================

/**
 * Lấy tất cả tham số lọc từ UI và gọi API tương ứng.
 * Ưu tiên dùng searchMovies nếu có lọc nâng cao hoặc đang ở trạng thái "All".
 * @param {string} status - Trạng thái cố định (Now Showing, Coming Soon, hoặc All).
 */
async function loadAndFilterMovies(status = currentFilter) {
    try {
        let res;

        // 1. TẠO REQUEST LỌC TỪ TẤT CẢ CÁC TRƯỜNG INPUT/SELECT
        const filterRequest = {
            title: titleSearchInput.value,
            genre: genreFilter.value,
            language: languageFilter.value,
            director: directorFilter.value,
            cast: castFilter.value,
        };

        // Kiểm tra xem có bất kỳ trường lọc nâng cao nào được điền hay không (ngoại trừ status)
        const isAdvancedFilterUsed = Object.values(filterRequest).some(value => value && value.trim() !== "");

        // 2. XÁC ĐỊNH HÀM API ĐỂ GỌI
        if (isAdvancedFilterUsed || status === "All") {
            // Trường hợp 1: Có lọc nâng cao HOẶC đang ở chế độ 'Tất cả'
            // Dùng API search để tận dụng bộ lọc đa chiều của Backend
            res = await movieApi.searchMovies(filterRequest);
        } else if (status === "Now Showing") {
            // Trường hợp 2: Chỉ lọc theo trạng thái 'Đang Chiếu' (không có filter nâng cao)
            res = await movieApi.getNowShowing();
        } else if (status === "Coming Soon") {
            // Trường hợp 3: Chỉ lọc theo trạng thái 'Sắp Chiếu' (không có filter nâng cao)
            res = await movieApi.getComingSoon();
        } else {
            // Trường hợp 4: Mặc định, lấy tất cả
            res = await movieApi.getAll();
        }

        // 3. XỬ LÝ KẾT QUẢ VÀ RENDER
        const movies = res?.result || res || [];
        allMovies = movies;

        renderMovies(movies);
    } catch (err) {
        console.error("❌ Load movies error:", err);
        movieList.innerHTML = `
            <div class="col-12 text-center text-danger">
                <p>Không thể tải danh sách phim! Vui lòng kiểm tra kết nối API.</p>
            </div>
        `;
    }
}


// ========================== RENDER MOVIES (GIỮ NGUYÊN) ==========================
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

        // ✅ ROBUST: Lấy ID từ entity field chuẩn (movieID) + fallbacks (Pascal, snake, id generic)
        let movieIdField = m.movieID || m.MovieID || m.id || m._id || m.movie_id;
        if (!movieIdField) {
            // Auto-find bất kỳ key nào chứa 'id' (case-insensitive)
            const idKey = Object.keys(m).find(key => key.toLowerCase().includes('id'));
            if (idKey) movieIdField = m[idKey];
        }

        // ✅ DEBUG LOG: Chạy filter → Check Console để xem structure m & ID extracted
        console.log(`DEBUG - Movie "${m.title || 'Unknown'}" structure:`, m);
        console.log(`DEBUG - Extracted ID: "${movieIdField}" (from key: ${movieIdField ? Object.keys(m).find(k => m[k] === movieIdField) : 'none'})`);

        card.innerHTML = `
            <div class="status-badge ${badgeClass}">${badgeText}</div>
            <div class="image-wrapper">
                <img src="${m.posterUrl || m.PosterUrl || "../image/default_movie.jpg"}" alt="${m.title || 'Unknown'}">
                <div class="action-overlay">
                    ${
            (m.status === "Coming Soon" || badgeText.includes("COMING"))
                ? `<button class="btn btn-lg btn-book-now btn-soon">XEM THÔNG BÁO</button>`
                : `<button class="btn btn-lg btn-book-now">ĐẶT VÉ NGAY</button>`
        }
                </div>
            </div>
            <div class="movie-info">
                <h5>${m.title || 'Unknown'}</h5>
                <p class="genre-list">${m.genre || "Chưa cập nhật thể loại"}</p>
            </div>
        `;

        card.addEventListener("click", () => {
            if (!movieIdField) {
                console.error("ID undefined - Full movie:", m);
                alert("Lỗi ID từ backend search. Thử 'Sắp chiếu' hoặc reload! (Check Console log).");
                return;
            }
            window.location.href = `movieDetail.html?id=${movieIdField}`;
        });

        movieList.appendChild(card);
    });
}
// ========================== BUTTON FILTERS & EVENT LISTENERS ==========================
function setActiveButton(activeBtn) {
    [btnNow, btnSoon, btnAll].forEach(btn => {
        btn.classList.remove("btn-custom-active");
        btn.classList.add("btn-custom-outline");
    });
    activeBtn.classList.remove("btn-custom-outline");
    activeBtn.classList.add("btn-custom-active");
}

// 1. Lắng nghe nút ĐANG CHIẾU
btnNow.addEventListener("click", () => {
    currentFilter = "Now Showing";
    setActiveButton(btnNow);
    // Gọi hàm lọc, giữ nguyên các filter nâng cao nếu có
    loadAndFilterMovies(currentFilter);
});

// 2. Lắng nghe nút SẮP CHIẾU
btnSoon.addEventListener("click", () => {
    currentFilter = "Coming Soon";
    setActiveButton(btnSoon);
    // Gọi hàm lọc, giữ nguyên các filter nâng cao nếu có
    loadAndFilterMovies(currentFilter);
});

// 3. Lắng nghe nút TẤT CẢ
btnAll.addEventListener("click", () => {
    currentFilter = "All";
    setActiveButton(btnAll);
    // Gọi hàm lọc, kích hoạt API search để áp dụng tất cả các filter đang có
    loadAndFilterMovies(currentFilter);
});

// 4. Lắng nghe nút TÌM KIẾM (Search Box)
searchByTitleBtn.addEventListener("click", () => {
    // Gọi hàm lọc, áp dụng tất cả các filter đang có
    loadAndFilterMovies(currentFilter);
});

// 5. Lắng nghe nút ÁP DỤNG LỌC (Advanced Filters)
applyFilterBtn.addEventListener("click", () => {
    // Gọi hàm lọc, áp dụng tất cả các filter đang có
    loadAndFilterMovies(currentFilter);
});


// 6. Lắng nghe sự kiện Enter trong ô tìm kiếm tên phim
titleSearchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        loadAndFilterMovies(currentFilter);
    }
});


// ========================== INIT ==========================
document.addEventListener("DOMContentLoaded", () => {
    loadAndFilterMovies(currentFilter);
});