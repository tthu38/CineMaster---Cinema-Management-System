import { revenueApi } from './api/revenueApi.js';
import { branchApi } from './api/branchApi.js';


/* ============================================================
  🚀 KHỞI TẠO TRANG
============================================================ */
document.addEventListener('DOMContentLoaded', init);


async function init() {
    const role = localStorage.getItem('role')?.toLowerCase();
    const branchId = localStorage.getItem('branchId');
    const branchSelect = document.getElementById('branchSelect');


    if (!role) {
        alert("Bạn chưa đăng nhập.");
        return;
    }


    console.log("🔍 Role:", role, "BranchID:", branchId);


    // 🏢 Nếu là admin -> chọn chi nhánh
    if (role === 'admin') {
        try {
            const branches = await branchApi.getAllActive();
            branchSelect.innerHTML =
                `<option value="">Tất cả chi nhánh</option>` +
                branches.map(b => `<option value="${b.branchId}">${b.branchName}</option>`).join('');


            // 🔁 Khi admin đổi chi nhánh → load lại chart + top movies
            branchSelect.addEventListener('change', async () => {
                const selectedBranch = branchSelect.value || null;
                console.log("📊 Reload chart & top movies for branch:", selectedBranch);
                await loadChart(selectedBranch);
                await loadTopMovies(selectedBranch);
            });
        } catch (err) {
            console.error("❌ Lỗi tải danh sách chi nhánh:", err);
        }
    } else {
        // 🧭 Manager / Staff -> chỉ xem chi nhánh của mình
        branchSelect.innerHTML = `<option value="${branchId}" selected>Chi nhánh của tôi</option>`;
        branchSelect.disabled = true;
    }


    const initialBranch = role === 'admin' ? (branchSelect.value || null) : branchId;
    await loadChart(initialBranch);
    await loadTopMovies(initialBranch);
}


/* ============================================================
  📊 LOAD & HIỂN THỊ BIỂU ĐỒ DOANH THU
============================================================ */
async function loadChart(branchId, filters = {}) {
    try {
        console.log("📤 Gửi request doanh thu:", { branchId, ...filters });
        let data;


        if (filters.from && filters.to) {
            data = await revenueApi.getByCustomRange(filters.from, filters.to, branchId);
        } else if (filters.year && filters.month) {
            data = await revenueApi.getByMonthDetail(filters.year, filters.month, branchId);
        } else {
            data = await revenueApi.getLast7Days(branchId);
        }


        if (!data || data.length === 0) {
            renderEmptyChart();
            return;
        }


        renderRevenueChart(data);
    } catch (err) {
        console.error("❌ Lỗi tải dữ liệu doanh thu:", err);
        renderEmptyChart();
    }
}


/* ============================================================
  🎨 RENDER BIỂU ĐỒ
============================================================ */
function renderRevenueChart(data) {
    const ctx = document.getElementById("revenueChart").getContext("2d");
    const labels = data.map(d => d.date);
    const revenues = data.map(d => d.revenue);


    if (window.revenueChartInstance) window.revenueChartInstance.destroy();


    const gradient = ctx.createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, "rgba(34,193,255,0.9)");
    gradient.addColorStop(1, "rgba(10,163,255,0.25)");


    window.revenueChartInstance = new Chart(ctx, {
        type: "bar",
        data: {
            labels,
            datasets: [{
                label: "Doanh thu (VNĐ)",
                data: revenues,
                backgroundColor: gradient,
                borderColor: "#22c1ff",
                borderWidth: 2,
                borderRadius: 8,
                hoverBackgroundColor: "#e50914"
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: "#0b1c35",
                    titleColor: "#22c1ff",
                    bodyColor: "#fff",
                    borderColor: "#22c1ff",
                    borderWidth: 1,
                    callbacks: {
                        label: ctx => `${Number(ctx.raw).toLocaleString("vi-VN")} ₫`
                    }
                }
            },
            scales: {
                x: { ticks: { color: "#9aa7b3" }, grid: { display: false } },
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: "#9aa7b3",
                        callback: v => v.toLocaleString("vi-VN") + " ₫"
                    },
                    grid: { color: "rgba(255,255,255,0.05)" }
                }
            }
        }
    });
}


/* ============================================================
  🕳️ KHÔNG CÓ DỮ LIỆU
============================================================ */
function renderEmptyChart() {
    const ctx = document.getElementById("revenueChart").getContext("2d");
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.font = "16px Roboto";
    ctx.fillStyle = "#9aa7b3";
    ctx.fillText("Không có dữ liệu doanh thu", 60, 60);
}


/* ============================================================
  🎬 LOAD TOP 10 PHIM
============================================================ */
async function loadTopMovies(branchId, filters = {}) {
    const tbody = document.getElementById('topMoviesBody');
    try {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Đang tải...</td></tr>`;


        const data = await revenueApi.getTopMovies(branchId, filters);


        if (!data || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Không có dữ liệu</td></tr>`;
            return;
        }


        tbody.innerHTML = data.map((item, idx) => `
           <tr>
               <td class="rank text-center">${idx + 1}</td>
               <td>${item.movieTitle}</td>
               <td class="text-center fw-bold text-info">${item.ticketsSold.toLocaleString('vi-VN')}</td>
           </tr>
       `).join('');
    } catch (err) {
        console.error("❌ Lỗi tải Top 10 phim:", err);
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
    }
}


/* ============================================================
  🔘 CÁC NÚT BỘ LỌC
============================================================ */


// 📅 Xem theo tháng
document.getElementById("btnViewMonthDetail").addEventListener("click", async () => {
    const monthInput = document.getElementById("monthInput").value;
    if (!monthInput) return alert("Vui lòng chọn tháng");


    const [year, month] = monthInput.split("-");
    const branchId = document.getElementById("branchSelect").value || null;


    await loadChart(branchId, { year, month });
    await loadTopMovies(branchId, { year, month });
});


// 📆 Lọc theo khoảng thời gian
document.getElementById("btnViewCustom").addEventListener("click", async () => {
    const from = document.getElementById("fromDate").value;
    const to = document.getElementById("toDate").value;
    const branchId = document.getElementById("branchSelect").value || null;


    if (!from || !to) return alert("Vui lòng chọn khoảng thời gian hợp lệ.");


    await loadChart(branchId, { from, to });
    await loadTopMovies(branchId, { from, to });
});


// 🗓️ 7 ngày gần nhất
document.getElementById("btnLast7Days").addEventListener("click", async () => {
    const branchId = document.getElementById("branchSelect").value || null;
    await loadChart(branchId);
    await loadTopMovies(branchId);
});

