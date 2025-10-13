import { seatTypeApi } from "./api.js";
import { seatApi } from "./api/seatApi.js";
import { auditoriumApi } from "./api/auditoriumApi.js";
import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
requireAuth();

// --- DOM ---
const seatForm = document.getElementById("seat-form");
const bulkSeatForm = document.getElementById("bulk-seat-form");
const bulkUpdateForm = document.getElementById("bulk-update-form");
const seatsBody = document.getElementById("seats-body");
const paginationControls = document.getElementById("pagination-controls");
const loadButton = document.getElementById("load-seats");

const diagramBranchSelect = document.getElementById("diagramBranchID");
const diagramAuditoriumSelect = document.getElementById("diagramAuditoriumID");
const seatDiagram = document.getElementById("seat-diagram");

const singleBranchSelect = document.getElementById("singleBranchID");
const auditoriumSelect = document.getElementById("auditoriumID");
const seatTypeSelect = document.getElementById("typeID");

const bulkBranchSelect = document.getElementById("bulkBranchID");
const bulkAuditoriumSelect = document.getElementById("bulkAuditoriumID");
const bulkTypeSelect = document.getElementById("bulkTypeID");

const updateBranchSelect = document.getElementById("updateBranchID");
const updateAuditoriumSelect = document.getElementById("updateAuditoriumID");
const newTypeSelect = document.getElementById("newTypeID");

const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");

let selectedSeatId = null;

// ======================= 1️⃣ LOAD DỮ LIỆU =======================
async function loadBranches() {
    try {
        const branches = await branchApi.getAll();
        const selects = [diagramBranchSelect, singleBranchSelect, bulkBranchSelect, updateBranchSelect];
        selects.forEach(sel => {
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Chi Nhánh ---</option>`;
            branches.forEach(b => sel.appendChild(new Option(b.branchName, b.branchID || b.branchId || b.id)));
        });
    } catch (err) { console.error("❌ Lỗi tải chi nhánh:", err); }
}

async function loadSeatTypes() {
    try {
        const types = await seatTypeApi.getAll();
        [seatTypeSelect, bulkTypeSelect, newTypeSelect].forEach(sel => {
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Loại Ghế ---</option>`;
            types.forEach(t => sel.appendChild(new Option(t.typeName, t.typeID)));
        });
    } catch (err) { console.error("❌ Lỗi tải loại ghế:", err); }
}

// ======================= 2️⃣ CẬP NHẬT PHÒNG CHIẾU =======================
async function updateAuditoriumOptions(branchSelect, branchId) {
    const map = {
        [diagramBranchSelect.id]: diagramAuditoriumSelect,
        [singleBranchSelect.id]: auditoriumSelect,
        [bulkBranchSelect.id]: bulkAuditoriumSelect,
        [updateBranchSelect.id]: updateAuditoriumSelect,
    };
    const target = map[branchSelect.id];
    if (!target) return;
    target.innerHTML = `<option value="" disabled selected hidden>--- Chọn Phòng Chiếu ---</option>`;
    if (!branchId) return;

    try {
        const auds = await auditoriumApi.getActiveByBranch(branchId);
        if (!auds?.length) {
            target.innerHTML += `<option disabled>(Không có phòng chiếu)</option>`;
            return;
        }
        auds.forEach(a => target.appendChild(new Option(a.name, a.auditoriumID)));
    } catch (err) { console.error("❌ Lỗi tải phòng chiếu:", err); }
}

// ======================= 3️⃣ SƠ ĐỒ GHẾ =======================
async function renderSeatDiagram(auditoriumId) {
    seatDiagram.innerHTML = `<p class="text-muted">Đang tải sơ đồ ghế...</p>`;
    try {
        const allSeats = await seatApi.getAll();
        const seats = allSeats.filter(s => s.auditoriumID === parseInt(auditoriumId));
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

                // ✅ CLICK GHẾ => LOAD FORM SỬA
                box.addEventListener("click", () => {
                    selectedSeatId = s.seatID;
                    loadSeatToForm(s);
                    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Đang chỉnh sửa: ${s.seatNumber}`;
                    submitBtn.innerHTML = `<i class="fa-solid fa-check me-2"></i> Cập Nhật Ghế`;
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

// ======================= 4️⃣ DANH SÁCH GHẾ =======================
async function loadSeats(page = 0, size = 10) {
    try {
        const data = await seatApi.getAll();
        renderSeatTable(data.slice(page * size, (page + 1) * size));
        renderPagination(data.length, page, size);
    } catch (err) { console.error("❌ Lỗi tải danh sách ghế:", err); }
}

function renderSeatTable(seats) {
    seatsBody.innerHTML = "";
    if (!seats?.length) {
        seatsBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Chưa có dữ liệu ghế</td></tr>`;
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
                <button class="btn btn-sm btn-warning me-2 btn-edit" data-id="${s.seatID}">
                    <i class="fa fa-pen"></i>
                </button>
                <button class="btn btn-sm btn-danger btn-delete" data-id="${s.seatID}">
                    <i class="fa fa-trash"></i>
                </button>
            </td>`;
        seatsBody.appendChild(tr);
    });

    // === Nút Edit ===
    document.querySelectorAll(".btn-edit").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const id = e.currentTarget.dataset.id;
            const seat = await seatApi.getById(id);
            loadSeatToForm(seat);
            formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Đang chỉnh sửa: ${seat.seatNumber}`;
            submitBtn.innerHTML = `<i class="fa-solid fa-check me-2"></i> Cập Nhật Ghế`;
            cancelBtn.style.display = "inline-block";
        });
    });

    // === Nút Delete ===
    document.querySelectorAll(".btn-delete").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const id = e.currentTarget.dataset.id;
            Swal.fire({
                title: "Xóa ghế?",
                text: "Hành động này không thể hoàn tác.",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Xóa",
                cancelButtonText: "Hủy",
            }).then(async res => {
                if (res.isConfirmed) {
                    await seatApi.delete(id);
                    Swal.fire("Đã xóa!", "", "success");
                    loadSeats();
                    if (diagramAuditoriumSelect.value)
                        renderSeatDiagram(diagramAuditoriumSelect.value);
                }
            });
        });
    });
}

function renderPagination(total, currentPage, size) {
    paginationControls.innerHTML = "";
    const totalPages = Math.ceil(total / size);
    for (let i = 0; i < totalPages; i++) {
        const btn = document.createElement("button");
        btn.className = `btn btn-sm ${i === currentPage ? "btn-primary" : "btn-outline-primary"} mx-1`;
        btn.textContent = i + 1;
        btn.addEventListener("click", () => loadSeats(i, size));
        paginationControls.appendChild(btn);
    }
}

// ======================= 5️⃣ LOAD FORM (CẬP NHẬT CHI NHÁNH / PHÒNG CHIẾU) =======================
async function loadSeatToForm(s) {
    // Gán giá trị cơ bản
    document.getElementById("seatID").value = s.seatID;
    document.getElementById("seatRow").value = s.seatRow;
    document.getElementById("columnNumber").value = s.columnNumber;
    document.getElementById("seatNumber").value = s.seatNumber;
    document.getElementById("status").value = s.status;

    // ✅ Load chi nhánh tương ứng
    if (s.branchID) {
        singleBranchSelect.value = s.branchID;
        // Khi chọn chi nhánh => load danh sách phòng chiếu của chi nhánh đó
        await updateAuditoriumOptions(singleBranchSelect, s.branchID);
    }

    // ✅ Gán phòng chiếu
    if (s.auditoriumID) {
        auditoriumSelect.value = s.auditoriumID;
    }

    // ✅ Gán loại ghế
    if (s.typeID) {
        seatTypeSelect.value = s.typeID;
    }

    // ✅ Đảm bảo trạng thái (Available/Broken/Reserved)
    const statusSelect = document.getElementById("status");
    if (statusSelect && s.status) {
        const val = s.status.charAt(0).toUpperCase() + s.status.slice(1).toLowerCase();
        statusSelect.value = val;
    }
}

// ======================= 6️⃣ HỦY SỬA =======================
cancelBtn.addEventListener("click", () => {
    seatForm.reset();
    document.getElementById("seatID").value = "";
    selectedSeatId = null;
    formTitle.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Thêm Ghế Ngồi Mới (Đơn Lẻ)`;
    submitBtn.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Tạo Ghế Ngồi`;
    cancelBtn.style.display = "none";
    document.querySelectorAll(".seat-box").forEach(el => el.classList.remove("seat-selected"));
});

// ======================= 7️⃣ SUBMIT FORM GHẾ ĐƠN =======================
seatForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = {
        auditoriumID: parseInt(auditoriumSelect.value),
        typeID: parseInt(seatTypeSelect.value),
        seatRow: document.getElementById("seatRow").value,
        columnNumber: parseInt(document.getElementById("columnNumber").value),
        seatNumber: document.getElementById("seatNumber").value,
        status: document.getElementById("status").value,
    };

    try {
        const id = document.getElementById("seatID").value;
        if (id) {
            await seatApi.update(id, data);
            Swal.fire("Cập nhật thành công!", "", "success");
        } else {
            await seatApi.create(data);
            Swal.fire("Thêm ghế thành công!", "", "success");
        }
        seatForm.reset();
        document.getElementById("seatID").value = "";
        selectedSeatId = null;
        formTitle.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Thêm Ghế Ngồi Mới (Đơn Lẻ)`;
        submitBtn.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Tạo Ghế Ngồi`;
        cancelBtn.style.display = "none";
        loadSeats();
        if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
    } catch (err) {
        Swal.fire("Lỗi khi lưu ghế!", err.message, "error");
    }
});

// ======================= 8️⃣ HÀNG LOẠT =======================
bulkSeatForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = {
        auditoriumID: parseInt(bulkAuditoriumSelect.value),
        typeID: parseInt(bulkTypeSelect.value),
        rowCount: parseInt(document.getElementById("rowCount").value),
        columnCount: parseInt(document.getElementById("columnCount").value),
        startRowChar: document.getElementById("startChar").value.trim().toUpperCase(),
    };
    await seatApi.createBulk(data);
    Swal.fire("Tạo hàng loạt thành công!", "", "success");
    loadSeats();
    if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
});

// ======================= 9️⃣ CẬP NHẬT HÀNG LOẠT =======================
bulkUpdateForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = {
        auditoriumID: parseInt(updateAuditoriumSelect.value),
        newTypeID: newTypeSelect.value ? parseInt(newTypeSelect.value) : null,
        newStatus: document.getElementById("newStatusSelect").value || null,
        seatRowToUpdate: document.getElementById("rowToUpdate").value.trim().toUpperCase(),
        isConvertCoupleSeat: document.getElementById("isConvertCoupleSeat").checked,
        isSeparateCoupleSeat: document.getElementById("isSeparateCoupleSeat").checked,
    };
    await seatApi.bulkUpdateRow(data);
    Swal.fire("Cập nhật hàng loạt thành công!", "", "success");
    loadSeats();
    if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
});

// ======================= 🔟 KHỞI TẠO =======================
[diagramBranchSelect, singleBranchSelect, bulkBranchSelect, updateBranchSelect].forEach(sel => {
    sel.addEventListener("change", e => updateAuditoriumOptions(e.target, e.target.value));
});
diagramAuditoriumSelect.addEventListener("change", e => renderSeatDiagram(e.target.value));
loadButton.addEventListener("click", () => loadSeats());

await loadBranches();
await loadSeatTypes();
await loadSeats();
