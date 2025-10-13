import { movieApi } from "./api/movieApi.js";

const form = document.getElementById("movie-form");

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    // 🔹 Gom dữ liệu từ form
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

    // 🔹 Chuẩn bị FormData đúng định dạng backend yêu cầu
    const formData = new FormData();
    formData.append("movie", new Blob([JSON.stringify(movie)], { type: "application/json" }));
    if (posterFile) formData.append("posterFile", posterFile);

    try {
        // ✅ Gọi API đã định nghĩa trong api.js
        const response = await movieApi.create(formData);

        Swal.fire("Thành công", "Đã tạo phim thành công!", "success")
            .then(() => window.location.href = "listMovies.html");

    } catch (err) {
        console.error("Create movie error:", err);
        Swal.fire("Lỗi", err.message || "Không thể tạo phim!", "error");
    }
});
