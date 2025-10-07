import { moviesApi } from "../js/api.js";

const form = document.getElementById("movie-form");

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
    // 👇 gửi JSON với content-type application/json
    formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));
    if (posterFile) formData.append("posterFile", posterFile);

    try {
        const res = await fetch("http://localhost:8080/api/v1/movies", {
            method: "POST",
            body: formData
        });

        const data = await res.json();
        if (data.code === 1000) {
            Swal.fire("Thành công", "Đã tạo phim", "success")
                .then(() => window.location.href = "listMovies.html");
        } else {
            Swal.fire("Lỗi", data.message, "error");
        }
    } catch (err) {
        console.error("Create movie error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tạo phim!", "error");
    }
});
