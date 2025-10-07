const API_BASE_URL = "http://localhost:8080/api/v1";

async function loadNews(category = "") {
    let url = `${API_BASE_URL}/news`;
    if (category) url += `?category=${category}`;

    const res = await fetch(url);
    const data = await res.json();
    const newsList = document.getElementById("news-list");
    newsList.innerHTML = "";

    data.result.forEach(news => {
        const col = document.createElement("div");
        col.className = "col-md-6 col-lg-4 d-flex";

        // Nếu tin active = true thì show sửa/xóa
        // Nếu active = false thì show nút khôi phục
        let actions = "";
        if (news.active) {
            actions = `
                <a href="updateNews.html?id=${news.newsID}" class="btn btn-sm btn-warning">
                    <i class="fas fa-edit"></i> Sửa
                </a>
                <button class="btn btn-sm btn-danger delete-btn" data-id="${news.newsID}">
                    <i class="fas fa-trash"></i> Xóa
                </button>`;
        } else {
            actions = `
                <button class="btn btn-sm btn-success restore-btn" data-id="${news.newsID}">
                    <i class="fas fa-undo"></i> Khôi phục
                </button>`;
        }

        col.innerHTML = `
          <div class="news-card flex-fill">
              <img src="${news.imageUrl}" alt="${news.title}">
              <div class="news-card-body d-flex flex-column">
                  <div class="flex-grow-1">
                      <h5>${news.title}</h5>
                      <p><small>${new Date(news.publishDate).toLocaleDateString()}</small></p>
                      <p>${news.content ? news.content.substring(0, 120) : ""}...</p>
                      <a href="newsDetail.html?id=${news.newsID}" class="read-more">Xem chi tiết</a>
                  </div>
                  <div class="news-actions mt-3">
                      ${actions}
                  </div>
              </div>
          </div>
        `;
        newsList.appendChild(col);
    });

    // Sự kiện xóa (set active=false)
    document.querySelectorAll(".delete-btn").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = btn.dataset.id;
            if (confirm("Bạn có chắc muốn xóa tin này?")) {
                try {
                    const res = await fetch(`${API_BASE_URL}/news/${id}`, { method: "DELETE" });
                    const result = await res.json();
                    if (result.code === 1000) {
                        alert("Xóa thành công!");
                        loadNews(category);
                    } else {
                        alert("Xóa thất bại: " + result.message);
                    }
                } catch (err) {
                    alert("Lỗi kết nối server");
                }
            }
        });
    });

    // Sự kiện khôi phục (set active=true)
    document.querySelectorAll(".restore-btn").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = btn.dataset.id;
            if (confirm("Bạn có chắc muốn khôi phục tin này?")) {
                try {
                    const res = await fetch(`${API_BASE_URL}/news/${id}/restore`, { method: "PUT" });
                    const result = await res.json();
                    if (result.code === 1000) {
                        alert("Khôi phục thành công!");
                        loadNews(category);
                    } else {
                        alert("Khôi phục thất bại: " + result.message);
                    }
                } catch (err) {
                    alert("Lỗi kết nối server");
                }
            }
        });
    });
}

// Sidebar category filter
document.querySelectorAll(".category-link").forEach(link => {
    link.addEventListener("click", e => {
        e.preventDefault();
        document.querySelectorAll(".category-link").forEach(l => l.classList.remove("active"));
        link.classList.add("active");
        loadNews(link.dataset.category);
    });
});

// Load mặc định
loadNews();
