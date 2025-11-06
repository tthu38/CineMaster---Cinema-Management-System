// ====================== IMPORT API ======================
import { aiSchedulerApi } from "./api/aiSchedulerApi.js";
import { movieApi } from "./api/movieApi.js";
import { auditoriumApi } from "./api/auditoriumApi.js";
import { branchApi } from "./api/branchApi.js";

// ====================== ELEMENTS ======================
const resultEl = document.getElementById("result");
const tableContainer = document.getElementById("table-container");
const branchSelect = document.getElementById("branchSelect");
let generatedData = [];

// ====================== INIT ======================
// ✅ Chạy khi DOM đã sẵn sàng
document.addEventListener("DOMContentLoaded", async () => {
    await loadBranches();
});

// ================== LOAD CHI NHÁNH ==================
async function loadBranches() {
    const branchSelect = document.getElementById("branchId");
    if (!branchSelect) {
        console.error("❌ Không tìm thấy phần tử select #branchId!");
        return;
    }

    branchSelect.innerHTML = `<option>⏳ Đang tải...</option>`;

    try {
        const branches = await branchApi.getNames();
        if (!Array.isArray(branches) || branches.length === 0) {
            branchSelect.innerHTML = `<option value="">(Không có chi nhánh khả dụng)</option>`;
            return;
        }

        branchSelect.innerHTML = branches.map(b => `
            <option value="${b.id || b.branchID}">
                ${b.branchName || b.name || `Chi nhánh #${b.id}`}
            </option>
        `).join("");
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
        branchSelect.innerHTML = `<option value="">(Lỗi tải chi nhánh)</option>`;
    }
}


// ====================== GỌI AI SINH LỊCH ======================
export async function generateSchedule() {
    const branchId = document.getElementById("branchId").value;
    const date = document.getElementById("date").value;
    const basePrice = Number(document.getElementById("basePrice").value);

    if (!branchId) return alert("Vui lòng chọn chi nhánh!");
    if (!date) return alert("Vui lòng chọn ngày chiếu!");

    resultEl.textContent = "⏳ Đang gọi AI tạo lịch chiếu...";
    tableContainer.innerHTML = "";

    try {
        const raw = await aiSchedulerApi.generate(branchId, date);
        if (!raw?.length) {
            resultEl.textContent = "⚠️ AI không trả về lịch chiếu nào.";
            return;
        }

        // Bổ sung thông tin phim + phòng
        generatedData = await Promise.all(
            raw.map(async (s) => {
                const movie = await movieApi.getById(s.movieId);
                const auditorium = await auditoriumApi.getById(s.auditoriumId);

                return {
                    ...s,
                    movieTitle: movie?.title || `Phim #${s.movieId}`,
                    auditoriumName: auditorium?.name || `Phòng #${s.auditoriumId}`,
                    language: s.language || (Math.random() > 0.5 ? "Vietnamese" : "English"),
                    price: s.price !== undefined ? s.price : basePrice,
                };
            })
        );

        renderTable(generatedData);
        resultEl.textContent = `✅ Đã sinh ${generatedData.length} suất chiếu.`;
    } catch (err) {
        console.error("❌ Lỗi:", err);
        resultEl.textContent = `❌ Lỗi: ${err.message}`;
    }
}

// ====================== RENDER TABLE ======================
function renderTable(data) {
    let html = `
    <table>
      <tr>
        <th>#</th>
        <th>🎞️ Phim</th>
        <th>🏟️ Phòng</th>
        <th>🗣️ Ngôn ngữ</th>
        <th>📅 Ngày</th>
        <th>⏰ Bắt đầu</th>
        <th>⏹️ Kết thúc</th>
        <th>💰 Giá vé (₫)</th>
        <th>✅ Xác nhận</th>
      </tr>`;

    data.forEach((s, i) => {
        const start = new Date(s.startTime);
        const end = new Date(s.endTime);

        html += `
      <tr>
        <td>${i + 1}</td>
        <td>${s.movieTitle}</td>
        <td>${s.auditoriumName}</td>
        <td>
          <select id="lang-${i}">
            <option value="Vietnamese" ${s.language === "Vietnamese" ? "selected" : ""}>Vietnamese</option>
            <option value="English" ${s.language === "English" ? "selected" : ""}>English</option>
          </select>
        </td>

        <td>${start.toISOString().split("T")[0]}</td>
        <td>${start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</td>
        <td>${end.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</td>
        <td>
          <input 
            type="number" 
            id="price-${i}" 
            value="${s.price}" 
            min="0" 
            step="1000" 
            style="width:90px;text-align:center">
        </td>
        <td>
          <button id="save-btn-${i}" class="confirm-btn" onclick="confirmShowtime(${i})">💾 Lưu</button>
        </td>
      </tr>`;
    });

    html += `
    </table>
    <div style="margin-top:15px;text-align:right">
      <button id="saveAllBtn" class="confirm-btn" style="background:#2563eb" onclick="confirmAll()">
        💾 Lưu tất cả
      </button>
    </div>
  `;

    tableContainer.innerHTML = html;
}

// ====================== LƯU 1 SUẤT ======================
window.confirmShowtime = async function (index) {
    const s = generatedData[index];
    if (!s) return;

    // Lấy dữ liệu thực từ form
    s.price = Number(document.getElementById(`price-${index}`).value);
    s.language = document.getElementById(`lang-${index}`).value;

    const button = document.querySelector(`#save-btn-${index}`);
    resultEl.textContent = "💾 Đang lưu suất chiếu...";

    try {
        await aiSchedulerApi.approveAI([{
            periodId: s.periodId || s.screeningPeriodId,
            auditoriumId: s.auditoriumId,
            startTime: s.startTime,
            endTime: s.endTime,
            language: s.language,
            price: s.price
        }]);

        resultEl.textContent = `✅ Đã lưu thành công: ${s.movieTitle}`;
        if (button) {
            button.textContent = "✅ Đã lưu";
            button.disabled = true;
            button.style.opacity = "0.6";
            button.style.cursor = "default";
        }
    } catch (err) {
        console.error(err);
        resultEl.textContent = `❌ Lỗi khi lưu: ${err.message}`;
    }
};

// ====================== LƯU TOÀN BỘ ======================
window.confirmAll = async function () {
    resultEl.textContent = "💾 Đang lưu tất cả lịch chiếu...";

    try {
        const payload = generatedData.map((s, i) => ({
            periodId: s.periodId || s.screeningPeriodId,
            auditoriumId: s.auditoriumId,
            startTime: s.startTime,
            endTime: s.endTime,
            language: document.getElementById(`lang-${i}`).value,
            price: Number(document.getElementById(`price-${i}`).value),
        }));

        console.log("📤 Payload gửi backend:", payload);
        await aiSchedulerApi.approveAI(payload);

        // Cập nhật giao diện sau khi lưu
        document.querySelectorAll(".confirm-btn").forEach(btn => {
            btn.textContent = "✅ Đã lưu";
            btn.disabled = true;
            btn.style.opacity = "0.6";
            btn.style.cursor = "default";
        });

        resultEl.textContent = "✅ Đã lưu toàn bộ lịch chiếu thành công!";
    } catch (err) {
        console.error(err);
        resultEl.textContent = `❌ Lỗi khi lưu tất cả: ${err.message}`;
    }
};


window.generateSchedule = generateSchedule;
