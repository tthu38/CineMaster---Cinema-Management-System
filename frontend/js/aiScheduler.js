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

document.addEventListener("DOMContentLoaded", async () => {
    await loadBranches();

    // ✅ Tự nhận chi nhánh đang mở từ trang showtime (lưu trong localStorage)
    const savedBranchId = localStorage.getItem("currentBranchId");
    if (savedBranchId) {
        const branchSelect = document.getElementById("branchId");
        if (branchSelect) branchSelect.value = savedBranchId;
        console.log("🏢 Đã tự chọn chi nhánh:", savedBranchId);
    }

    // ✅ Lấy ngày được truyền qua query và set vào input (sau khi DOM đã load)
    const urlParams = new URLSearchParams(window.location.search);
    const passedDate = urlParams.get("date");
    const dateInput = document.getElementById("date");

    if (passedDate && dateInput) {
        dateInput.value = passedDate;
        console.log("📅 Ngày chiếu được nhận:", passedDate);
    }
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
        <th>🗑️ Xóa</th>
      </tr>`;

    data.forEach((s, i) => {
        const start = new Date(s.startTime);
        const end = new Date(s.endTime);

        html += `
      <tr id="row-${i}">
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
          <input type="number" id="price-${i}" value="${s.price}" min="0" step="1000" style="width:90px;text-align:center">
        </td>
        <td>
          <button id="save-btn-${i}" class="confirm-btn" onclick="confirmShowtime(${i})">💾 Lưu</button>
        </td>
        <td>
          <button id="delete-btn-${i}" class="delete-btn" onclick="deleteShowtime(${i})">🗑️ Xóa</button>
        </td>
      </tr>`;
    });

    // ✅ Thêm nút "Lưu tất cả" (KHÔNG dùng onclick)
    html += `
    </table>
    <div style="margin-top:15px;text-align:right">
        <button id="saveAllBtn" class="confirm-btn" style="background:#2563eb">
            💾 Lưu tất cả
        </button>
    </div>`;

    // ✅ Cập nhật vào DOM
    tableContainer.innerHTML = html;

    // ✅ Gắn lại event mỗi khi render bảng
    const saveAllBtn = document.getElementById("saveAllBtn");
    if (saveAllBtn) {
        saveAllBtn.addEventListener("click", confirmAll);
    }
}

// ====================== LƯU 1 SUẤT ======================
window.confirmShowtime = async function (index) {
    const s = generatedData[index];
    if (!s) return;

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

        // ✅ Gắn cờ đã lưu
        s.isSaved = true;

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

window.deleteShowtime = function (index) {
    const s = generatedData[index];
    if (!s) return;

    if (!confirm(`Bạn có chắc muốn xóa suất chiếu "${s.movieTitle}" (${s.auditoriumName})?`)) return;

    // ✅ Xóa khỏi mảng
    generatedData.splice(index, 1);

    // ✅ Re-render toàn bộ bảng để cập nhật index và id
    renderTable(generatedData);
    window.scrollTo(0, scrollY);
    resultEl.textContent = `🗑️ Đã xóa lịch chiếu "${s.movieTitle}".`;
};



window.confirmAll = async function () {
    resultEl.textContent = "💾 Đang lưu tất cả lịch chiếu...";

    try {
        // ✅ Lọc bỏ các suất đã được lưu (isSaved = true)
        const unsaved = generatedData.filter(s => !s.isSaved);
        if (unsaved.length === 0) {
            resultEl.textContent = "ℹ️ Tất cả suất chiếu đã được lưu trước đó.";
            return;
        }

        const payload = unsaved.map((s, i) => ({
            periodId: s.periodId || s.screeningPeriodId,
            auditoriumId: s.auditoriumId,
            startTime: s.startTime,
            endTime: s.endTime,
            language: document.getElementById(`lang-${generatedData.indexOf(s)}`).value,
            price: Number(document.getElementById(`price-${generatedData.indexOf(s)}`).value),
        }));

        console.log("📤 Payload gửi backend:", payload);
        await aiSchedulerApi.approveAI(payload);

        // ✅ Đánh dấu tất cả là đã lưu
        unsaved.forEach(s => s.isSaved = true);

        // ✅ Cập nhật giao diện
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
