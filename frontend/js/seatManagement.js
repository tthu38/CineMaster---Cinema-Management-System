import { seatApi } from "./api/seatApi.js";
import { branchApi } from "./api/branchApi.js";
import { auditoriumApi } from "./api/auditoriumApi.js";
import { seatTypeApi } from "./api.js";
import { requireAuth } from "./api/config.js";
requireAuth();
// ====================== ROLE DETECTION ======================
const role = localStorage.getItem("role") || null;
const branchId = localStorage.getItem("branchId") || null;

const isAdmin = role === "Admin";
const isManager = role === "Manager";
const isStaff = role === "Staff";
const isCustomer = role === "Customer" || role === "Guest" || !role;

// Nếu không có quyền, chặn truy cập
if (!isAdmin && !isManager && !isStaff) {
    Swal.fire("🚫 Truy cập bị từ chối", "Bạn không có quyền xem trang này.", "error")
        .then(() => (window.location.href = "/home/index.html"));
}


/* ======================== DOM ======================== */
const seatDiagram = document.getElementById("seat-diagram");
const seatsBody = document.getElementById("seats-body");
const paginationControls = document.getElementById("pagination");
const loadButton = document.getElementById("load-seats");
const diagramBranchSelect = document.getElementById("diagramBranchID");
const diagramAuditoriumSelect = document.getElementById("diagramAuditoriumID");

// --- Các form ---
const formSingle = document.getElementById("seat-form");
const formBulk = document.getElementById("bulk-seat-form");
const formBulkUpdate = document.getElementById("bulk-update-form");

async function loadBranches() {
    try {
        const branches = await branchApi.getAll();
        const allBranchSelects = [
            diagramBranchSelect,
            document.getElementById("singleBranchID"),
            document.getElementById("bulkBranchID"),
            document.getElementById("updateBranchID"),
        ];
        allBranchSelects.forEach(sel => {
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Chi Nhánh ---</option>`;
        });

        const visibleBranches = isAdmin
            ? branches
            : branches.filter(b => String(b.branchId) === String(branchId));

        allBranchSelects.forEach(sel => {
            visibleBranches.forEach(b => sel.appendChild(new Option(b.branchName, b.branchId)));
        });
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
    }
}

async function loadSeatTypes() {
    try {
        const types = await seatTypeApi.getAll();
        ["typeID", "bulkTypeID", "newTypeID"].forEach(id => {
            const sel = document.getElementById(id);
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Loại Ghế ---</option>`;
            types.forEach(t => sel.appendChild(new Option(t.typeName, t.typeID)));
        });
    } catch (err) {
        console.error("❌ Lỗi tải loại ghế:", err);
    }
}

async function updateAuditoriumOptions(branchId) {
    const numericId = parseInt(branchId);
    if (isNaN(numericId)) return;

    // Xác định nơi gọi để tránh double append
    const activeElement = document.activeElement;
    const isDiagramSelect = activeElement === diagramBranchSelect;

    const selectors = isDiagramSelect
        ? [diagramAuditoriumSelect]
        : [
            document.getElementById("auditoriumID"),
            document.getElementById("bulkAuditoriumID"),
            document.getElementById("updateAuditoriumID"),
        ];

    selectors.forEach(sel => {
        sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Phòng Chiếu ---</option>`;
    });

    try {
        const auds = await auditoriumApi.getActiveByBranch(numericId);
        if (!auds?.length) {
            selectors.forEach(sel => sel.innerHTML += `<option disabled>(Không có phòng chiếu)</option>`);
            return;
        }
        auds.forEach(a => {
            selectors.forEach(sel => {
                if (![...sel.options].some(opt => opt.value == a.auditoriumID)) {
                    sel.appendChild(new Option(a.auditoriumName || a.name, a.auditoriumID));
                }
            });
        });
    } catch (err) {
        console.error("❌ Lỗi tải phòng chiếu:", err);
    }
}

/* ======================== SƠ ĐỒ GHẾ ======================== */
async function renderSeatDiagram(auditoriumId) {
    seatDiagram.innerHTML = `<p class="text-muted">Đang tải sơ đồ ghế...</p>`;
    try {
        const seats = await seatApi.getByAuditorium(auditoriumId);
        if (!seats.length) {
            seatDiagram.innerHTML = `<p class="text-center text-muted">Chưa có ghế trong phòng chiếu này.</p>`;
            return;
        }

        const grouped = {};
        seats.forEach(s => {
            if (!grouped[s.seatRow]) grouped[s.seatRow] = [];
            grouped[s.seatRow].push(s);
        });

        seatDiagram.innerHTML = `<div class="screen">MÀN HÌNH</div>`;
        Object.keys(grouped).sort().forEach(row => {
            const rowDiv = document.createElement("div");
            rowDiv.className = "seat-row";

            const label = document.createElement("div");
            label.className = "seat-label";
            label.textContent = row;
            rowDiv.appendChild(label);

            grouped[row]
                .sort((a, b) => a.columnNumber - b.columnNumber)
                .forEach(s => {
                    const box = document.createElement("div");
                    const status = s.status?.toLowerCase();

                    box.className = `seat-box seat-type-${s.typeName.toLowerCase()}`;
                    if (status === "reserved") box.classList.add("seat-reserved");
                    if (status === "broken") box.classList.add("seat-broken");

                    const labelText = `${s.seatRow}${s.seatNumber}`;
                    box.textContent = status === "broken" ? "❌" : labelText;
                    box.title = `${labelText} - ${s.typeName} (${s.status})`;

                    // ✅ Click đổi trạng thái trực tiếp (phân quyền)
                    if (isAdmin || isManager || isStaff) {
                        box.addEventListener("click", async () => {
                            const seat = await seatApi.getById(s.seatID);
                            const currentStatus = seat.status?.toLowerCase();
                            const next = currentStatus === "available" ? "Broken" : "Available";

                            // ✅ Staff/Manager chỉ được đổi ghế trong chi nhánh của mình
                            const seatBranchId = seat.branchId || seat.branchID || seat.branch?.branchId || seat.auditorium?.branchId;
                            if (!isAdmin && String(seatBranchId) !== String(branchId)) {
                                return Swal.fire("🚫 Không thể đổi trạng thái", "Bạn chỉ được phép chỉnh ghế của chi nhánh mình.", "error");
                            }


                            const confirm = await Swal.fire({
                                title: `Ghế ${seat.seatRow}${seat.seatNumber}`,
                                text: `Bạn có muốn đổi trạng thái thành "${next}" không?`,
                                icon: "question",
                                showCancelButton: true,
                                confirmButtonColor: next === "Broken" ? "#e50914" : "#22c1ff",
                                confirmButtonText: "Xác nhận",
                                cancelButtonText: "Hủy",
                            });
                            if (!confirm.isConfirmed) return;

                            await seatApi.update(seat.seatID, {
                                auditoriumID: seat.auditoriumID,
                                typeID: seat.typeID,
                                seatRow: seat.seatRow,
                                seatNumber: seat.seatNumber,
                                columnNumber: seat.columnNumber,
                                status: next,
                            });

                            Swal.fire({
                                title: "✅ Cập nhật thành công!",
                                text: `Ghế ${seat.seatRow}${seat.seatNumber} hiện đã "${next}".`,
                                icon: "success",
                                timer: 1500,
                                showConfirmButton: false,
                            });

                            await loadSeatsByAuditorium(auditoriumId);
                            await renderSeatDiagram(auditoriumId);
                        });
                    }


                    rowDiv.appendChild(box);
                });
            seatDiagram.appendChild(rowDiv);
        });
    } catch (err) {
        seatDiagram.innerHTML = `<p class="text-danger">Không thể tải sơ đồ ghế.</p>`;
        console.error("❌ Lỗi hiển thị sơ đồ ghế:", err);
    }
}

/* ======================== DANH SÁCH GHẾ ======================== */
async function loadSeats(page = 0, size = 10) {
    const data = await seatApi.getAll();
    renderSeatTable(data.slice(page * size, (page + 1) * size));
    renderPagination(data.length, page, size);
}

async function loadSeatsByAuditorium(auditoriumId, page = 0, size = 10) {
    if (!auditoriumId) return;
    const data = await seatApi.getByAuditorium(auditoriumId);
    renderSeatTable(data.slice(page * size, (page + 1) * size));
    renderPagination(data.length, page, size, auditoriumId);
}

function renderSeatTable(seats) {
    seatsBody.innerHTML = "";
    if (!seats?.length) {
        seatsBody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Chưa có dữ liệu ghế</td></tr>`;
        return;
    }

    seats.forEach(s => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${s.seatID}</td>
            <td>${s.branchName || "—"}</td>
            <td>${s.auditoriumName || "—"}</td>
            <td>${s.seatRow}</td>
            <td>${s.columnNumber}</td>
            <td>${s.typeName}</td>
            <td>
                <span class="badge ${
            s.status === "Broken"
                ? "bg-danger"
                : s.status === "Reserved"
                    ? "bg-warning text-dark"
                    : "bg-success"
        }">${s.status}</span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-warning btn-toggle-status" data-id="${s.seatID}">
                    Đổi
                </button>
            </td>
        `;
        seatsBody.appendChild(tr);
    });

    // 🎯 Sự kiện đổi trạng thái trong bảng
    document.querySelectorAll(".btn-toggle-status").forEach(btn => {
        btn.addEventListener("click", async e => {
            const id = e.currentTarget.dataset.id;
            const seat = await seatApi.getById(id);
            const next = seat.status === "Available" ? "Broken" : "Available";

            await seatApi.update(id, {
                auditoriumID: seat.auditoriumID,
                typeID: seat.typeID,
                seatRow: seat.seatRow,
                seatNumber: seat.seatNumber,
                columnNumber: seat.columnNumber,
                status: next,
            });

            await Swal.fire("✅ Cập nhật thành công!", `Ghế ${seat.seatRow}${seat.seatNumber} → ${next}`, "success");

            const currentAuditorium = diagramAuditoriumSelect.value;
            if (currentAuditorium) {
                await loadSeatsByAuditorium(currentAuditorium);
                await renderSeatDiagram(currentAuditorium);
            } else {
                await loadSeats();
            }
        });
    });
}

/* ======================== PAGINATION ======================== */
function renderPagination(total, currentPage, size, auditoriumId = null) {
    paginationControls.innerHTML = "";
    const totalPages = Math.ceil(total / size);
    if (totalPages <= 1) return;

    const createBtn = (page, label, disabled = false, active = false) => `
        <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
                ${disabled ? "disabled" : ""}
                onclick="goToSeatPage(${page}, ${auditoriumId || "null"})">${label}</button>
    `;

    const maxVisible = 5;
    const start = Math.floor(currentPage / maxVisible) * maxVisible;
    const end = Math.min(start + maxVisible, totalPages);

    paginationControls.innerHTML += createBtn(Math.max(start - 1, 0), "&laquo;", currentPage === 0);
    for (let i = start; i < end; i++) {
        paginationControls.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    }
    paginationControls.innerHTML += createBtn(Math.min(end, totalPages - 1), "&raquo;", currentPage >= totalPages - 1);
}

window.goToSeatPage = (page, auditoriumId) => {
    if (auditoriumId && auditoriumId !== "null") loadSeatsByAuditorium(auditoriumId, page);
    else loadSeats(page);
};

/* ======================== FORM HANDLERS ======================== */
// 1️⃣ Thêm ghế đơn
formSingle.addEventListener("submit", async e => {
    e.preventDefault();
    const auditoriumID = parseInt(document.getElementById("auditoriumID").value);
    const typeID = parseInt(document.getElementById("typeID").value);
    const seatRow = document.getElementById("seatRow").value.trim().toUpperCase();
    const columnNumber = parseInt(document.getElementById("columnNumber").value);
    const seatNumber = document.getElementById("seatNumber").value.trim();
    const status = document.getElementById("status").value || "Available";

    if (!auditoriumID || !typeID || !seatRow || !columnNumber || !seatNumber)
        return Swal.fire("⚠️ Thiếu thông tin", "Vui lòng nhập đầy đủ dữ liệu.", "warning");

    await seatApi.create({ auditoriumID, typeID, seatRow, columnNumber, seatNumber, status });
    Swal.fire("✅ Thành công", `Ghế ${seatRow}${seatNumber} đã được tạo.`, "success");
    await loadSeatsByAuditorium(auditoriumID);
    await renderSeatDiagram(auditoriumID);
    formSingle.reset();
});

// 2️⃣ Tạo hàng loạt ghế
formBulk.addEventListener("submit", async e => {
    e.preventDefault();
    const auditoriumID = parseInt(document.getElementById("bulkAuditoriumID").value);
    const typeID = parseInt(document.getElementById("bulkTypeID").value);
    const startChar = document.getElementById("startChar").value.trim().toUpperCase();
    const rowCount = parseInt(document.getElementById("rowCount").value);
    const columnCount = parseInt(document.getElementById("columnCount").value);

    if (!auditoriumID || !typeID || !startChar || !rowCount || !columnCount)
        return Swal.fire("⚠️ Thiếu thông tin", "Vui lòng nhập đầy đủ dữ liệu.", "warning");

    await seatApi.createBulk({ auditoriumID, typeID, startChar, rowCount, columnCount });
    Swal.fire("✅ Thành công", "Đã tạo sơ đồ ghế hàng loạt.", "success");
    await loadSeatsByAuditorium(auditoriumID);
    await renderSeatDiagram(auditoriumID);
    formBulk.reset();
});

// 3️⃣ Cập nhật loại/trạng thái theo dãy
formBulkUpdate.addEventListener("submit", async e => {
    e.preventDefault();
    const auditoriumID = parseInt(document.getElementById("updateAuditoriumID").value);
    const rowToUpdate = document.getElementById("rowToUpdate").value.trim().toUpperCase();
    const newTypeID = parseInt(document.getElementById("newTypeID").value) || null;
    const newStatus = document.getElementById("newStatusSelect").value || null;

    if (!auditoriumID || !rowToUpdate)
        return Swal.fire("⚠️ Thiếu dữ liệu", "Vui lòng chọn phòng và dãy ghế.", "warning");

    await seatApi.bulkUpdateRow({ auditoriumID, rowToUpdate, newTypeID, newStatus });
    Swal.fire("✅ Thành công", `Đã cập nhật dãy ${rowToUpdate}.`, "success");
    await loadSeatsByAuditorium(auditoriumID);
    await renderSeatDiagram(auditoriumID);
    formBulkUpdate.reset();
});
/* ======================== KHỞI TẠO ======================== */
await loadBranches();
await loadSeatTypes();

if (isManager || isStaff) {
    ["card-add-seat", "card-bulk-seat", "card-update-seat"]
        .forEach(id => document.getElementById(id)?.classList.add("d-none"));
}

/* ======================== TỰ ĐỘNG KHỞI TẠO ======================== */
if (isAdmin) {
    // 🟢 Admin có thể xem tất cả ghế
    await loadSeats();
    seatDiagram.innerHTML = `<p class="text-center text-muted">Chọn phòng chiếu để xem sơ đồ ghế.</p>`;
}
else if (isManager || isStaff) {
    // 🟡 Staff / Manager
    diagramBranchSelect.value = branchId;
    diagramBranchSelect.disabled = true; // Khóa không cho đổi chi nhánh

    // Gọi load phòng chiếu của chi nhánh đó
    await updateAuditoriumOptions(branchId);

    // ✅ Kiểm tra có phòng không
    const auds = await auditoriumApi.getActiveByBranch(branchId);
    if (auds && auds.length > 0) {
        // 🧩 Đổ danh sách phòng vào select
        diagramAuditoriumSelect.innerHTML = `<option value="" disabled selected hidden>--- Chọn Phòng Chiếu ---</option>`;
        auds.forEach(a => {
            diagramAuditoriumSelect.appendChild(new Option(a.auditoriumName || a.name, a.auditoriumID));
        });
        seatDiagram.innerHTML = `<p class="text-center text-info">Vui lòng chọn phòng chiếu để xem sơ đồ ghế.</p>`;
    } else {
        seatDiagram.innerHTML = `<p class="text-center text-muted">Chi nhánh này chưa có phòng chiếu nào.</p>`;
    }
}
else {
    // 🔴 Không có quyền
    Swal.fire("🚫 Truy cập bị từ chối", "Bạn không có quyền xem trang này.", "error")
        .then(() => (window.location.href = "/home/index.html"));
}

/* ======================== GẮN SỰ KIỆN ======================== */
[
    diagramBranchSelect,
    document.getElementById("singleBranchID"),
    document.getElementById("bulkBranchID"),
    document.getElementById("updateBranchID"),
].forEach(sel => sel?.addEventListener("change", e => updateAuditoriumOptions(e.target.value)));

diagramAuditoriumSelect.addEventListener("change", async e => {
    const id = e.target.value;
    if (!id) return;
    await renderSeatDiagram(id);
    await loadSeatsByAuditorium(id);
});

loadButton.addEventListener("click", async () => {
    const current = diagramAuditoriumSelect.value;
    if (current) {
        await loadSeatsByAuditorium(current);
        await renderSeatDiagram(current);
    } else {
        await loadSeats();
    }
});
