import { movieApi } from "./api/movieApi.js";
const API_BASE_URL = "http://localhost:8080/api/v1";

const form = document.getElementById("movie-form");

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
        window.previewTrailer(url);
        status.textContent = "✅ Upload thành công! Link đã được thêm tự động.";
    } catch (err) {
        console.error("Trailer upload error:", err);
        status.textContent = "⚠️ Lỗi khi upload trailer!";
    }
});

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

    // Nếu không hợp lệ
    container.innerHTML = "<p>Không thể preview trailer.</p>";
};

// ========================== SUBMIT CREATE ==========================
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
        await movieApi.create(formData);
        Swal.fire("Thành công", "Đã tạo phim thành công!", "success")
            .then(() => window.location.href = "listMovies.html");
    } catch (err) {
        console.error("Create movie error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tạo phim!", "error");
    }
});
