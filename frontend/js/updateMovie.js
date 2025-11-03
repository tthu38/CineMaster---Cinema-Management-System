import { movieApi } from "./api/movieApi.js";
const API_BASE_URL = "http://localhost:8080/api/v1";

const form = document.getElementById("movie-form");
const params = new URLSearchParams(window.location.search);
const id = params.get("id");

// ☁️ Upload trailer lên Cloudinary
document.getElementById("trailerFile").addEventListener("change", async function () {
    const file = this.files[0];
    if (!file) return;

    const status = document.getElementById("uploadStatus");
    status.textContent = "⏳ Đang tải trailer lên Cloudinary...";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const res = await fetch(`${API_BASE_URL}/trailers/upload`, {
            method: "POST",
            body: formData
        });

        if (!res.ok) {
            status.textContent = "❌ Upload thất bại!";
            return;
        }

        const url = await res.text();
        document.getElementById("trailerUrl").value = url;
        previewTrailer(url);
        status.textContent = "✅ Upload thành công! Link đã được thêm tự động.";
    } catch (err) {
        console.error(err);
        status.textContent = "⚠️ Lỗi khi upload trailer!";
    }
});

// ========================== CHECK ID ==========================
if (!id || id === "undefined" || isNaN(Number(id))) {
    Swal.fire("Lỗi", "Không tìm thấy ID phim hợp lệ để cập nhật!", "error")
        .then(() => window.location.href = "listMovies.html");
} else {
    console.log("🎬 Đang load phim ID:", id);
    loadMovie();
}

// ========================== LOAD MOVIE ==========================
async function loadMovie() {
    try {
        const response = await movieApi.getById(id);
        const m = response?.result || response;
        if (!m || !m.title) throw new Error("Không tìm thấy phim!");

        // ✅ Gán dữ liệu vào form
        document.getElementById("title").value = m.title ?? "";
        document.getElementById("genre").value = m.genre ?? "";
        document.getElementById("duration").value = m.duration ?? "";
        document.getElementById("releaseDate").value = m.releaseDate ?? "";
        document.getElementById("director").value = m.director ?? "";
        document.getElementById("cast").value = m.cast ?? "";
        document.getElementById("description").value = m.description ?? "";
        document.getElementById("language").value = m.language ?? "";
        document.getElementById("ageRestriction").value = m.ageRestriction ?? "";
        document.getElementById("country").value = m.country ?? "";
        document.getElementById("trailerUrl").value = m.trailerUrl ?? "";
        document.getElementById("status").value = m.status ?? "";

        // Preview poster + trailer
        if (m.posterUrl) document.getElementById("poster-preview").src = m.posterUrl;
        if (m.trailerUrl) previewTrailer(m.trailerUrl);

        console.log("🎬 Movie loaded:", m);
    } catch (err) {
        console.error("❌ Load movie error:", err);
        await Swal.fire("Lỗi", err.message || "Không thể tải thông tin phim!", "error");
        window.location.href = "listMovies.html";
    }
}

// ========================== PREVIEW TRAILER ==========================
window.previewTrailer = function (url) {
    if (!url) return;
    const container = document.getElementById("trailer-container");
    if (!container) return;

    // Nếu là YouTube
    if (url.includes("youtube.com") || url.includes("youtu.be")) {
        const reg = /^.*(youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=)([^#&?]*).*/;
        const m = url.match(reg);
        if (m && m[2].length === 11) {
            const videoId = m[2];
            container.innerHTML = `
                <iframe id="trailer-preview" width="100%" height="315"
                    src="https://www.youtube.com/embed/${videoId}?autoplay=1&mute=0&controls=1"
                    allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen>
                </iframe>`;
            return;
        }
    }

    // Nếu là Cloudinary video
    if (url.endsWith(".mp4") || url.includes("cloudinary")) {
        container.innerHTML = `
            <video id="trailer-preview" controls autoplay style="width:100%; border-radius:12px;">
                <source src="${url}" type="video/mp4">
                Trình duyệt của bạn không hỗ trợ video.
            </video>`;
        return;
    }

    container.innerHTML = "<p>Không thể preview trailer.</p>";
};

// ========================== PREVIEW POSTER ==========================
window.previewPosterFile = function (input) {
    const f = input.files[0];
    if (f) {
        const r = new FileReader();
        r.onload = e => document.getElementById("poster-preview").src = e.target.result;
        r.readAsDataURL(f);
    }
};

// ========================== SUBMIT UPDATE ==========================
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const movie = {
        title: document.getElementById("title").value.trim(),
        genre: document.getElementById("genre").value.trim(),
        duration: parseInt(document.getElementById("duration").value),
        releaseDate: document.getElementById("releaseDate").value,
        director: document.getElementById("director").value.trim(),
        cast: document.getElementById("cast").value.trim(),
        description: document.getElementById("description").value.trim(),
        language: document.getElementById("language").value.trim(),
        ageRestriction: document.getElementById("ageRestriction").value.trim(),
        country: document.getElementById("country").value.trim(),
        trailerUrl: document.getElementById("trailerUrl").value.trim(),
        status: document.getElementById("status").value.trim()
    };

    const posterFile = document.getElementById("posterFile").files[0];
    const formData = new FormData();
    formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));
    if (posterFile) formData.append("posterFile", posterFile);

    try {
        await movieApi.update(id, formData);
        Swal.fire("Thành công", "Phim đã được cập nhật!", "success")
            .then(() => window.location.href = "listMovies.html");
    } catch (err) {
        console.error("Update movie error:", err);
        Swal.fire("Lỗi", err.message || "Không thể cập nhật phim!", "error");
    }
});
