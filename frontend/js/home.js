// ====== Cấu hình cơ bản ======
const LOGIN_PAGE = "../user/login.html";
const TOKEN_KEY = "accessToken";
const HOME_PAGE = "home.html";
const API_BASE_URL = "http://localhost:8080/api/v1/auth";

// ====== Logout ======
window.handleLogout = async function handleLogout() {
    try {
        const token = localStorage.getItem("accessToken");
        if (token) {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: "POST",
                headers: { "Authorization": `Bearer ${token}` }
            });
        }
    } catch (e) {
        console.error("Logout error:", e);
    } finally {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userInfo");
        window.location.href = HOME_PAGE;
    }
};

// ====== Ẩn/hiện nút Logout theo trạng thái đăng nhập ======
document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    const isLoggedIn = !!localStorage.getItem(TOKEN_KEY);
    const btn = document.querySelector(".logout-btn");
    if (btn) {
        btn.classList.toggle("d-none", !isLoggedIn);
    }

    // Gọi API load phim
    loadMovies();
    loadComingSoon();
});
async function loadMovies() {
    try {
        const res = await fetch(`${API_BASE_URL}/now-showing`); // 👈 bỏ /movies
        const data = await res.json();
        console.log("Now showing:", data);

        if (data.code === 1000 && Array.isArray(data.result)) {
            const container = document.querySelector("#now-showing");
            container.innerHTML = data.result.map(m => `
                <div class="movie-card">
                    <img src="${m.posterUrl}" alt="${m.title}">
                    <h3>${m.title}</h3>
                    <p>${m.genre}</p>
                </div>
            `).join("");
        }
    } catch (err) {
        console.error("Lỗi load phim:", err);
    }
}

async function loadComingSoon() {
    try {
        const res = await fetch(`${API_BASE_URL}/coming-soon`); // 👈 bỏ /movies
        const data = await res.json();
        console.log("Coming soon:", data);

        if (data.code === 1000 && Array.isArray(data.result)) {
            const container = document.querySelector("#coming-soon");
            container.innerHTML = data.result.map(m => `
                <div class="movie-card">
                    <img src="${m.posterUrl}" alt="${m.title}">
                    <h3>${m.title}</h3>
                    <p>${m.genre}</p>
                </div>
            `).join("");
        }
    } catch (err) {
        console.error("Lỗi load phim:", err);
    }
}

document.addEventListener("DOMContentLoaded", () => {

    loadMovies();
    loadComingSoon();
    loadHeroMovies();
});
// Hero: load tất cả phim
async function loadHeroMovies() {
    try {
        const res = await fetch(`${API_BASE_URL}`); // lấy all phim
        const data = await res.json();

        if (data.code === 1000 && Array.isArray(data.result)) {
            const container = document.querySelector("#hero-carousel");
            container.innerHTML = data.result.map(m => `
                <div class="carousel-item">
                    <img src="${m.posterUrl}" alt="${m.title}">
                </div>
            `).join("");

            items = document.querySelectorAll('.carousel-item');
            if (items.length > 0) {
                current = 0;
                updateSlides();
            }
        }
    } catch (err) {
        console.error("Lỗi load hero:", err);
    }
}
async function loadTodayShowtimes() {
    try {
        const res = await fetch("http://localhost:8080/api/v1/auth/showtimes/today");
        const showtimes = await res.json();

        const container = document.querySelector(".showtimes");
        container.innerHTML = "";

        // Group theo movie
        const grouped = {};
        showtimes.forEach(st => {
            if (!grouped[st.movieTitle]) {
                grouped[st.movieTitle] = {
                    branch: st.branchName,
                    times: []
                };
            }
            grouped[st.movieTitle].times.push(st.startTime);
        });

        Object.entries(grouped).forEach(([title, info]) => {
            const card = document.createElement("div");
            card.classList.add("showtime-card");

            card.innerHTML = `
                <h3>${title}</h3>
                <p>${info.branch}</p>
                <div class="times">
                    ${info.times.map(t => `<span>${t}</span>`).join("")}
                </div>
            `;

            container.appendChild(card);
        });
    } catch (err) {
        console.error("Error loading showtimes:", err);
    }
}

document.addEventListener("DOMContentLoaded", loadTodayShowtimes);




