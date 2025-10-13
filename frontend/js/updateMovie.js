// /js/updateMovie.js
import { movieApi } from "./api/movieApi.js";

const form = document.getElementById("movie-form");
const params = new URLSearchParams(window.location.search);
const id = params.get("id");

// ========================== CHECK ID ==========================
if (!id || id === "undefined" || isNaN(Number(id))) {
    Swal.fire("Lỗi", "Không tìm thấy ID phim hợp lệ để cập nhật!", "error")
        .then(() => window.location.href = "listMovies.html");
} else {
    console.log("🎬 Đang load phim ID:", id);
    loadMovie();
}

// ========================== YOUTUBE EMBED ==========================
function getEmbedUrl(url, unmute = false) {
    if (!url) return "";
    const reg = /^.*(youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=)([^#&?]*).*/;
    const m = url.match(reg);
    if (m && m[2].length === 11) {
        const videoId = m[2];
        const muteParam = unmute ? "0" : "1";
        return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=${muteParam}&controls=0&modestbranding=1&rel=0&loop=1&playlist=${videoId}`;
    }
    return "";
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
window.previewTrailer = function (url, unmute = false) {
    const embedUrl = getEmbedUrl(url, unmute);
    document.getElementById("trailer-preview").src = embedUrl;
};

window.toggleSound = function () {
    const url = document.getElementById("trailerUrl").value;
    const iframe = document.getElementById("trailer-preview");
    const isMuted = iframe.src.includes("mute=1");
    previewTrailer(url, isMuted);
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
