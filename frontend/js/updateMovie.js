import { moviesApi } from "../js/api.js";

const form = document.getElementById("movie-form");
const params = new URLSearchParams(window.location.search);
const id = params.get("id");

// Hàm convert URL Youtube sang embed
function getEmbedUrl(url){
    const reg=/^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=)([^#&?]*).*/;
    const m=url.match(reg);
    if (m && m[2].length===11) {
        const videoId = m[2];
        return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=0&controls=0&modestbranding=1&rel=0&loop=1&playlist=${videoId}`;
    }
    return "";
}


// Load movie data
async function loadMovie() {
    try {
        const m = await moviesApi.getById(id);
        if (m) {
            document.getElementById("title").value = m.title;
            document.getElementById("genre").value = m.genre;
            document.getElementById("duration").value = m.duration;
            document.getElementById("releaseDate").value = m.releaseDate;
            document.getElementById("director").value = m.director;
            document.getElementById("cast").value = m.cast;
            document.getElementById("description").value = m.description;
            document.getElementById("language").value = m.language;
            document.getElementById("ageRestriction").value = m.ageRestriction;
            document.getElementById("country").value = m.country;
            document.getElementById("trailerUrl").value = m.trailerUrl;
            document.getElementById("status").value = m.status;

            if (m.posterUrl) {
                document.getElementById("poster-preview").src = m.posterUrl;
            }
            if (m.trailerUrl) {
                previewTrailer(m.trailerUrl); // 👈 load xong là preview luôn
            }
        }
    } catch (err) {
        console.error("Load movie error:", err);
        Swal.fire("Lỗi", "Không thể tải phim!", "error");
    }
}
loadMovie();

// Preview trailer khi nhập lại
window.previewTrailer = function(url, unmute = false) {
    const embedUrl = getEmbedUrl(url, unmute);
    document.getElementById("trailer-preview").src = embedUrl;
};
// Gắn nút toggle bật/tắt tiếng
window.toggleSound = function() {
    const url = document.getElementById("trailerUrl").value;
    const iframe = document.getElementById("trailer-preview");
    const isMuted = iframe.src.includes("mute=1");
    previewTrailer(url, isMuted); // reload lại với mute = 0
};

// Preview poster
window.previewPosterFile = function(input) {
    const f = input.files[0];
    if (f) {
        const r = new FileReader();
        r.onload = e => document.getElementById('poster-preview').src = e.target.result;
        r.readAsDataURL(f);
    }
};

// Submit update
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const movie = {
        title: document.getElementById("title").value,
        genre: document.getElementById("genre").value,
        duration: parseInt(document.getElementById("duration").value),
        releaseDate: document.getElementById("releaseDate").value,
        director: document.getElementById("director").value,
        cast: document.getElementById("cast").value,
        description: document.getElementById("description").value,
        language: document.getElementById("language").value,
        ageRestriction: document.getElementById("ageRestriction").value,
        country: document.getElementById("country").value,
        trailerUrl: document.getElementById("trailerUrl").value,
        status: document.getElementById("status").value
    };

    const posterFile = document.getElementById("posterFile").files[0];

    const formData = new FormData();
    formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));
    if (posterFile) formData.append("posterFile", posterFile);

    try {
        const res = await fetch(`http://localhost:8080/api/v1/movies/${id}`, {
            method: "PUT",
            body: formData
        });

        const data = await res.json();
        if (data.code === 1000) {
            Swal.fire("Thành công", "Đã cập nhật phim", "success")
                .then(() => window.location.href = "listMovies.html");
        } else {
            Swal.fire("Lỗi", data.message, "error");
        }
    } catch (err) {
        console.error("Update movie error:", err);
        Swal.fire("Lỗi", err.message || "Không thể cập nhật!", "error");
    }
});
