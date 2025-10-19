import { seatApi } from "./api/seatApi.js";
import { auditoriumApi } from "./api/auditoriumApi.js";
import { requireAuth } from "./api/config.js";

// Đảm bảo người dùng đã đăng nhập và có token
requireAuth();

// ======================================================================================
// ✅ LẤY THÔNG TIN CHI NHÁNH TỪ LOCAL STORAGE SAU KHI ĐĂNG NHẬP
// ======================================================================================
const USER_BRANCH_ID = parseInt(localStorage.getItem("branchId"));
const USER_BRANCH_NAME = localStorage.getItem("branchName") || "Không xác định";

// Kiểm tra nhanh, nếu không có ID chi nhánh
if (!USER_BRANCH_ID || isNaN(USER_BRANCH_ID)) {
    // Thông báo lỗi và chuyển hướng để đảm bảo an toàn dữ liệu
    Swal.fire("Lỗi truy cập!", "Không tìm thấy ID chi nhánh. Vui lòng đăng nhập lại.", "error");
    // Có thể chuyển hướng về trang login
    // window.location.href = "../home/login.html";
}

// Cập nhật tên chi nhánh lên tiêu đề
document.getElementById("staff-branch-name").textContent = `(Chi Nhánh: ${USER_BRANCH_NAME})`;


// --- DOM Cần thiết cho Staff ---
const seatForm = document.getElementById("seat-form");
const bulkUpdateForm = document.getElementById("bulk-update-form");
const seatsBody = document.getElementById("seats-body");
const paginationControls = document.getElementById("pagination-controls");
const loadButton = document.getElementById("load-seats");

const diagramAuditoriumSelect = document.getElementById("diagramAuditoriumID");
const updateAuditoriumSelect = document.getElementById("updateAuditoriumID");
const seatDiagram = document.getElementById("seat-diagram");

const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");
const currentSeatInfoInput = document.getElementById("currentSeatInfo");

let selectedSeatId = null;

// ======================= 1️⃣ LOAD PHÒNG CHIẾU (CHỈ CỦA CHI NHÁNH NÀY) =======================
async function loadAuditoriums() {
    try {
        // ✅ CHỈ LẤY PHÒNG CHIẾU TỪ USER_BRANCH_ID
        const auds = await auditoriumApi.getActiveByBranch(USER_BRANCH_ID);
        const selects = [diagramAuditoriumSelect, updateAuditoriumSelect];

        selects.forEach(target => {
            target.innerHTML = `<option value="" disabled selected hidden>--- Chọn Phòng Chiếu ---</option>`;
            if (!auds?.length) {
                target.innerHTML += `<option disabled>(Không có phòng chiếu)</option>`;
                return;
            }
            auds.forEach(a => target.appendChild(new Option(a.name, a.auditoriumID)));
        });
    } catch (err) {
        console.error("❌ Lỗi tải phòng chiếu:", err);
        Swal.fire("Lỗi!", "Không thể tải danh sách phòng chiếu. Vui lòng kiểm tra API: /auditorium/branch/{id}", "error");
    }
}

// ======================= 2️⃣ SƠ ĐỒ GHẾ =======================
async function renderSeatDiagram(auditoriumId) {
    if (!auditoriumId) {
        seatDiagram.innerHTML = `<p class="text-center" style="color:var(--muted)">Vui lòng chọn Phòng Chiếu để hiển thị sơ đồ.</p>`;
        return;
    }

    seatDiagram.innerHTML = `<p class="text-muted">Đang tải sơ đồ ghế...</p>`;
    try {
        // Tải tất cả ghế và lọc theo AuditoriumId (Giả định seatApi.getAll() trả về cả chi nhánh)
        const allSeats = await seatApi.getAll();
        const seats = allSeats.filter(s => s.auditoriumID === parseInt(auditoriumId) && s.branchID === USER_BRANCH_ID);

        if (!seats.length) {
            seatDiagram.innerHTML = `<p class="text-center text-muted">Chưa có ghế trong phòng chiếu này.</p>`;
            return;
        }

        const grouped = {};
        seats.forEach(s => {
            if (!grouped[s.seatRow]) grouped[s.seatRow] = [];
            grouped[s.seatRow].push(s);
        });

        seatDiagram.innerHTML = `<div class="screen">Màn hình</div>`;
        Object.keys(grouped).sort().forEach(row => {
            const rowDiv = document.createElement("div");
            rowDiv.className = "seat-row";
            const label = document.createElement("div");
            label.className = "seat-label";
            label.textContent = row;
            rowDiv.appendChild(label);

            grouped[row].sort((a, b) => a.columnNumber - b.columnNumber).forEach(s => {
                const box = document.createElement("div");
                box.className = `seat-box seat-type-${s.typeName.toLowerCase()}`;
                const status = s.status?.toLowerCase();
                if (status === "reserved") box.classList.add("seat-reserved");
                if (status === "broken") box.classList.add("seat-broken");
                if (s.seatID === selectedSeatId) box.classList.add("seat-selected");

                box.textContent = s.seatNumber;
                box.title = `${s.seatNumber} - ${s.typeName} (${s.status})`;

                // ✅ CLICK GHẾ => LOAD FORM CẬP NHẬT TRẠNG THÁI
                box.addEventListener("click", () => {
                    selectedSeatId = s.seatID;
                    loadSeatToForm(s);
                    currentSeatInfoInput.value = `${s.seatNumber} (${s.typeName}, ID:${s.seatID})`;
                    submitBtn.disabled = false;
                    cancelBtn.style.display = "inline-block";

                    document.querySelectorAll(".seat-box").forEach(el => el.classList.remove("seat-selected"));
                    box.classList.add("seat-selected");
                });

                rowDiv.appendChild(box);
            });
            seatDiagram.appendChild(rowDiv);
        });
    } catch (err) {
        seatDiagram.innerHTML = `<p class="text-danger">Không thể tải sơ đồ ghế.</p>`;
        console.error("❌ Lỗi hiển thị sơ đồ ghế:", err);
    }
}

// ======================= 3️⃣ DANH SÁCH GHẾ (LỌC THEO CHI NHÁNH) =======================
async function loadSeats(page = 0, size = 10) {
    try {
        // Tải tất cả ghế (hoặc API mới: getAllByBranch)
        const allData = await seatApi.getAll();

        // ✅ LỌC THEO BRANCH ID CỦA NGƯỜI DÙNG
        const seatsByBranch = allData.filter(s => s.branchID === USER_BRANCH_ID);

        renderSeatTable(seatsByBranch.slice(page * size, (page + 1) * size));
        renderPagination(seatsByBranch.length, page, size);
    } catch (err) {
        console.error("❌ Lỗi tải danh sách ghế:", err);
        Swal.fire("Lỗi!", "Không thể tải danh sách ghế.", "error");
    }
}

function renderSeatTable(seats) {
    seatsBody.innerHTML = "";
    if (!seats?.length) {
        seatsBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Chưa có dữ liệu ghế trong chi nhánh này.</td></tr>`;
        return;
    }
    seats.forEach(s => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${s.seatID}</td>
            <td>${s.auditoriumName || "?"}</td>
            <td>${s.seatRow}/${s.columnNumber}</td>
            <td>${s.seatNumber}</td>
            <td>${s.typeName}</td>
            <td>${s.status}</td>
            <td>
                <button class="btn btn-sm btn-warning btn-edit" data-id="${s.seatID}">
                    <i class="fa fa-pen"></i> Sửa TT
                </button>
            </td>`;
        seatsBody.appendChild(tr);
    });

    // === Nút Edit (Chỉ còn cập nhật trạng thái) ===
    document.querySelectorAll(".btn-edit").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const id = e.currentTarget.dataset.id;
            const seat = await seatApi.getById(id);
            loadSeatToForm(seat);
            currentSeatInfoInput.value = `${seat.seatNumber} (${seat.typeName}, ID:${seat.seatID})`;
            submitBtn.disabled = false;
            cancelBtn.style.display = "inline-block";
        });
    });
}

function renderPagination(total, currentPage, size) {
    paginationControls.innerHTML = "";
    const totalPages = Math.ceil(total / size);
    // ... (Giữ nguyên logic phân trang) ...
    for (let i = 0; i < totalPages; i++) {
        const btn = document.createElement("button");
        btn.className = `btn btn-sm ${i === currentPage ? "btn-primary" : "btn-outline-primary"} mx-1`;
        btn.textContent = i + 1;
        btn.addEventListener("click", () => loadSeats(i, size));
        paginationControls.appendChild(btn);
    }
}

// ======================= 4️⃣ LOAD FORM CHỈ CẬP NHẬT TRẠNG THÁI =======================
function loadSeatToForm(s) {
    // Lưu tạm các giá trị cần thiết cho API Update (API yêu cầu đầy đủ các trường)
    document.getElementById("seatID").value = s.seatID;
    document.getElementById("auditoriumID").value = s.auditoriumID;
    document.getElementById("typeID").value = s.typeID;
    document.getElementById("seatRow").value = s.seatRow;
    document.getElementById("columnNumber").value = s.columnNumber;
    document.getElementById("seatNumber").value = s.seatNumber;

    // ✅ Gán Trạng Thái để chỉnh sửa
    const statusSelect = document.getElementById("status");
    if (statusSelect && s.status) {
        statusSelect.value = s.status.charAt(0).toUpperCase() + s.status.slice(1).toLowerCase();
    }
}

// ======================= 5️⃣ HỦY SỬA =======================
cancelBtn.addEventListener("click", () => {
    seatForm.reset();
    document.getElementById("seatID").value = "";
    selectedSeatId = null;
    currentSeatInfoInput.value = "Chưa chọn ghế";
    submitBtn.disabled = true;
    cancelBtn.style.display = "none";
    document.querySelectorAll(".seat-box").forEach(el => el.classList.remove("seat-selected"));
});


// ======================= 6️⃣ SUBMIT FORM GHẾ ĐƠN (CẬP NHẬT TRẠNG THÁI) =======================
seatForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = document.getElementById("seatID").value;

    if (!id) {
        Swal.fire("Lỗi!", "Vui lòng chọn ghế từ sơ đồ hoặc danh sách để cập nhật.", "warning");
        return;
    }

    // Lấy TẤT CẢ các giá trị (kể cả hidden) để gửi lên API update
    const data = {
        auditoriumID: parseInt(document.getElementById("auditoriumID").value),
        typeID: parseInt(document.getElementById("typeID").value),
        seatRow: document.getElementById("seatRow").value,
        columnNumber: parseInt(document.getElementById("columnNumber").value),
        seatNumber: document.getElementById("seatNumber").value,
        status: document.getElementById("status").value, // ✅ CHỈ CẦN THAY ĐỔI STATUS MỚI
    };

    try {
        await seatApi.update(id, data);
        Swal.fire("Cập nhật thành công!", "Trạng thái ghế đã được thay đổi.", "success");

        // Reset form
        seatForm.reset();
        document.getElementById("seatID").value = "";
        selectedSeatId = null;
        currentSeatInfoInput.value = "Chưa chọn ghế";
        submitBtn.disabled = true;
        cancelBtn.style.display = "none";

        loadSeats();
        if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
    } catch (err) {
        Swal.fire("Lỗi khi lưu ghế!", err.message, "error");
    }
});


// ======================= 7️⃣ CẬP NHẬT HÀNG LOẠT (CHỈ TRẠNG THÁI) =======================
bulkUpdateForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = {
        auditoriumID: parseInt(updateAuditoriumSelect.value),
        // Các trường này đã bị loại bỏ/cố định trong HTML Staff
        newTypeID: null,
        isConvertCoupleSeat: false,
        isSeparateCoupleSeat: false,

        // ✅ CHỈ CẬP NHẬT STATUS VÀ ROW
        newStatus: document.getElementById("newStatusSelect").value,
        seatRowToUpdate: document.getElementById("rowToUpdate").value.trim().toUpperCase(),
    };

    if (!data.newStatus) {
        Swal.fire("Lỗi!", "Vui lòng chọn Trạng thái Mới.", "warning");
        return;
    }

    try {
        // ✅ Cần Backend đảm bảo rowToUpdate chỉ áp dụng cho ghế thuộc auditoriumID đã chọn
        await seatApi.bulkUpdateRow(data);
        Swal.fire("Cập nhật hàng loạt thành công!", "", "success");
        loadSeats();
        if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
    } catch (err) {
        console.error("Lỗi bulk update:", err);
        Swal.fire("Lỗi khi cập nhật hàng loạt!", err.message, "error");
    }
});


// ======================= 8️⃣ KHỞI TẠO =======================
diagramAuditoriumSelect.addEventListener("change", e => renderSeatDiagram(e.target.value));
updateAuditoriumSelect.addEventListener("change", () => bulkUpdateForm.reset());
loadButton.addEventListener("click", () => loadSeats());

// Chạy khởi tạo
await loadAuditoriums();
await loadSeats();