import { statisticApi } from "./api/statisticApi.js";

// Elements
const rangeSelect = document.getElementById("revenueRange");
const reloadBtn = document.getElementById("btnReload");
const totalEl = document.getElementById("totalRevenue");
const ctx = document.getElementById("revenueChart");

// Chart instance
let chart = null;

// ====== Khởi động ======
window.addEventListener("DOMContentLoaded", async () => {
    await loadRevenue();
});

// ====== Sự kiện ======
reloadBtn.addEventListener("click", async () => {
    await loadRevenue();
});

// ====== Hàm chính ======
async function loadRevenue() {
    try {
        const data = await statisticApi.getRevenue();
        if (!data || !Array.isArray(data)) throw new Error("Không có dữ liệu.");

        // lọc theo phạm vi chọn
        const range = rangeSelect.value;
        let filtered = [...data];
        if (range !== "auto") {
            const days = parseInt(range);
            const cutoff = new Date();
            cutoff.setDate(cutoff.getDate() - days);
            filtered = data.filter(d => new Date(d.date) >= cutoff);
        }

        renderChart(filtered);
    } catch (err) {
        console.error("Lỗi khi tải doanh thu:", err);
        alert(err.message || "Không tải được thống kê doanh thu");
    }
}

// ====== Render Chart ======
function renderChart(data) {
    const labels = data.map(d => d.date);
    const values = data.map(d => d.totalRevenue);

    const total = values.reduce((a, b) => a + b, 0);
    totalEl.textContent = total.toLocaleString("vi-VN") + " VND";

    if (chart) chart.destroy();

    chart = new Chart(ctx, {
        type: "line",
        data: {
            labels,
            datasets: [
                {
                    label: "Doanh thu (VND)",
                    data: values,
                    borderColor: "#0dcaf0",
                    backgroundColor: "rgba(13,202,240,0.2)",
                    borderWidth: 2,
                    tension: 0.3,
                    fill: true,
                    pointRadius: 4,
                    pointBackgroundColor: "#ffc107",
                },
            ],
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    labels: { color: "#e6f1ff" },
                },
            },
            scales: {
                x: {
                    ticks: { color: "#e6f1ff" },
                    grid: { color: "rgba(255,255,255,0.1)" },
                },
                y: {
                    ticks: {
                        color: "#e6f1ff",
                        callback: val => val.toLocaleString("vi-VN"),
                    },
                    grid: { color: "rgba(255,255,255,0.1)" },
                },
            },
        },
    });
}
