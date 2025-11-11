// ==========================
// 🎬 Revenue Dashboard Script
// ==========================
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

    if (!role) return alert("Bạn chưa đăng nhập.");
    if (!localStorage.getItem('accessToken')) {
        window.location.href = '/home/login.html';
        return;
    }

    if (role === 'admin') {
        await loadBranches(branchSelect);
        branchSelect.addEventListener('change', async () => {
            const selected = branchSelect.value || null;
            await refreshDashboard(selected);
        });
    } else {
        branchSelect.innerHTML = `<option value="${branchId}" selected>Chi nhánh của tôi</option>`;
        branchSelect.disabled = true;
    }

    const initialBranch = role === 'admin' ? (branchSelect.value || null) : branchId;
    await refreshDashboard(initialBranch);
}

/* ============================================================
  🏢 LOAD DANH SÁCH CHI NHÁNH (Admin)
============================================================ */
async function loadBranches(selectEl) {
    try {
        const branches = await branchApi.getAllActive();
        selectEl.innerHTML =
            `<option value="">Tất cả chi nhánh</option>` +
            branches.map(b => `<option value="${b.branchId}">${b.branchName}</option>`).join('');
    } catch (err) {
        console.error("❌ Lỗi tải danh sách chi nhánh:", err);
        selectEl.innerHTML = `<option value="">Không thể tải chi nhánh</option>`;
    }
}

/* ============================================================
  📊 CẬP NHẬT TOÀN BỘ DASHBOARD
============================================================ */
async function refreshDashboard(branchId, filters = {}) {
    await loadChart(branchId, filters);
    await loadTopMovies(branchId, filters);
}

/* ============================================================
  📈 BIỂU ĐỒ DOANH THU
============================================================ */
async function loadChart(branchId, filters = {}) {
    const ctx = document.getElementById("revenueChart")?.getContext("2d");
    if (!ctx) return console.warn("⚠️ Không tìm thấy canvas #revenueChart");

    try {
        let data;
        if (filters.from && filters.to)
            data = await revenueApi.getByCustomRange(filters.from, filters.to, branchId);
        else if (filters.year && filters.month)
            data = await revenueApi.getByMonthDetail(filters.year, filters.month, branchId);
        else
            data = await revenueApi.getLast7Days(branchId);

        if (!data || data.length === 0) return renderEmptyChart(ctx);

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
    } catch (err) {
        console.error("❌ Lỗi tải dữ liệu doanh thu:", err);
        renderEmptyChart(ctx);
    }
}

function renderEmptyChart(ctx) {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.font = "16px Roboto";
    ctx.fillStyle = "#9aa7b3";
    ctx.fillText("Không có dữ liệu doanh thu", 60, 60);
}

/* ============================================================
  🎬 TOP 10 PHIM
============================================================ */
async function loadTopMovies(branchId, filters = {}) {
    const tbody = document.getElementById('topMoviesBody');
    if (!tbody) return;

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
  🔘 NÚT BỘ LỌC
============================================================ */
document.getElementById("btnViewMonthDetail")?.addEventListener("click", async () => {
    const monthInput = document.getElementById("monthInput").value;
    if (!monthInput) return alert("Vui lòng chọn tháng");
    const [year, month] = monthInput.split("-");
    const branchId = document.getElementById("branchSelect").value || null;
    await refreshDashboard(branchId, { year, month });
});

document.getElementById("btnViewCustom")?.addEventListener("click", async () => {
    const from = document.getElementById("fromDate").value;
    const to = document.getElementById("toDate").value;
    if (!from || !to) return alert("Vui lòng chọn khoảng thời gian hợp lệ.");
    const branchId = document.getElementById("branchSelect").value || null;
    await refreshDashboard(branchId, { from, to });
});

document.getElementById("btnLast7Days")?.addEventListener("click", async () => {
    const branchId = document.getElementById("branchSelect").value || null;
    await refreshDashboard(branchId);
});
