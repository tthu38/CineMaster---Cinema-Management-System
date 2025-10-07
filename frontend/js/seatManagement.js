import { seatApi, seatTypeApi, auditoriumApi, branchApi, requireAuth } from "./api.js";

// 🔐 Kiểm tra đăng nhập
requireAuth();

// --- DOM ELEMENTS ---
const seatForm = document.getElementById("seat-form");
const bulkSeatForm = document.getElementById("bulk-seat-form");
const bulkUpdateForm = document.getElementById("bulk-update-form");
const seatsBody = document.getElementById("seats-body");
const paginationControls = document.getElementById("pagination-controls");
const loadButton = document.getElementById("load-seats");

// Dropdowns cho sơ đồ ghế
const diagramBranchSelect = document.getElementById("diagramBranchID");
const diagramAuditoriumSelect = document.getElementById("diagramAuditoriumID");
const seatDiagram = document.getElementById("seat-diagram");

// Dropdowns cho thêm đơn lẻ
const singleBranchSelect = document.getElementById("singleBranchID");
const auditoriumSelect = document.getElementById("auditoriumID");
const seatTypeSelect = document.getElementById("typeID");

// Dropdowns cho tạo hàng loạt
const bulkBranchSelect = document.getElementById("bulkBranchID");
const bulkAuditoriumSelect = document.getElementById("bulkAuditoriumID");
const bulkTypeSelect = document.getElementById("bulkTypeID");

// Dropdowns cho cập nhật hàng loạt
const updateBranchSelect = document.getElementById("updateBranchID");
const updateAuditoriumSelect = document.getElementById("updateAuditoriumID");
const newTypeSelect = document.getElementById("newTypeID");

// ======================= 1️⃣ LOAD DỮ LIỆU BAN ĐẦU =======================
async function loadBranches() {
    try {
        const branches = await branchApi.getAll(); // 🔥 DÙNG getAll() THAY CHO getAllBranches()
        const selects = [diagramBranchSelect, singleBranchSelect, bulkBranchSelect, updateBranchSelect];
        selects.forEach(sel => {
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Chi Nhánh ---</option>`;
            branches.forEach(b => sel.appendChild(new Option(b.branchName, b.id)));
        });
        console.log("🌿 Danh sách chi nhánh (dropdown):", branches);
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
    }
}


async function loadSeatTypes() {
    try {
        const types = await seatTypeApi.getAll();
        [seatTypeSelect, bulkTypeSelect, newTypeSelect].forEach(sel => {
            sel.innerHTML = `<option value="" disabled selected hidden>--- Chọn Loại Ghế ---</option>`;
            types.forEach(t => sel.appendChild(new Option(t.typeName, t.typeID)));
        });
    } catch (err) {
        console.error("❌ Lỗi tải loại ghế:", err);
    }
}

// ======================= 2️⃣ CẬP NHẬT PHÒNG CHIẾU THEO CHI NHÁNH =======================
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
        console.log("📡 Gọi API: /auditoriums/branch/" + branchId);
        const auds = await auditoriumApi.getByBranch(branchId);
        console.log("🎬 Phòng chiếu nhận được:", auds);

        if (!auds || auds.length === 0) {
            target.innerHTML += `<option disabled>(Không có phòng chiếu)</option>`;
            return;
        }

        auds.forEach(a => target.appendChild(new Option(a.name, a.auditoriumID)));
    } catch (err) {
        console.error("❌ Lỗi tải phòng chiếu:", err);
    }
}

// ======================= 3️⃣ HIỂN THỊ SƠ ĐỒ GHẾ =======================
async function renderSeatDiagram(auditoriumId) {
    seatDiagram.innerHTML = `<p class="text-muted">Đang tải sơ đồ ghế...</p>`;
    try {
        const seats = await seatApi.getAll();
        const filtered = seats.filter(s => s.auditoriumID === parseInt(auditoriumId));

        if (filtered.length === 0) {
            seatDiagram.innerHTML = `<p class="text-center text-muted">Chưa có ghế nào trong phòng chiếu này.</p>`;
            return;
        }

        const grouped = {};
        filtered.forEach(s => {
            if (!grouped[s.seatRow]) grouped[s.seatRow] = [];
            grouped[s.seatRow].push(s);
        });

        seatDiagram.innerHTML = `<div class="screen">Màn hình</div>`;
        Object.keys(grouped)
            .sort()
            .forEach(row => {
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
                        box.className = "seat-box seat-type-" + s.typeName.toLowerCase();
                        if (s.status === "Reserved") box.classList.add("seat-reserved");
                        if (s.status === "Broken") box.classList.add("seat-broken");
                        box.textContent = s.seatNumber;
                        box.title = `${s.seatNumber} - ${s.typeName} (${s.status})`;
                        box.addEventListener("click", () => loadSeatToForm(s));
                        rowDiv.appendChild(box);
                    });
                seatDiagram.appendChild(rowDiv);
            });
    } catch (err) {
        console.error("❌ Lỗi hiển thị sơ đồ ghế:", err);
        seatDiagram.innerHTML = `<p class="text-danger">Không thể tải sơ đồ ghế.</p>`;
    }
}

// ======================= 4️⃣ DANH SÁCH GHẾ + PHÂN TRANG =======================
async function loadSeats(page = 0, size = 10) {
    try {
        const data = await seatApi.getAll();
        renderSeatTable(data.slice(page * size, (page + 1) * size));
        renderPagination(data.length, page, size);
    } catch (err) {
        console.error("❌ Lỗi tải danh sách ghế:", err);
    }
}

function renderSeatTable(seats) {
    seatsBody.innerHTML = "";
    if (!seats || seats.length === 0) {
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
                <button class="btn btn-sm btn-warning me-2" data-id="${s.seatID}">
                    <i class="fa fa-pen"></i>
                </button>
                <button class="btn btn-sm btn-danger" data-id="${s.seatID}">
                    <i class="fa fa-trash"></i>
                </button>
            </td>
        `;
        seatsBody.appendChild(tr);
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

// ======================= 5️⃣ FORM GHẾ ĐƠN =======================
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
        loadSeats();
        if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
    } catch (err) {
        console.error("❌ Lỗi lưu ghế:", err);
        Swal.fire("Lỗi khi lưu ghế!", err.message, "error");
    }
});

// ======================= 6️⃣ FORM HÀNG LOẠT =======================
bulkSeatForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = {
        auditoriumID: parseInt(bulkAuditoriumSelect.value),
        typeID: parseInt(bulkTypeSelect.value),
        rowCount: parseInt(document.getElementById("rowCount").value),
        columnCount: parseInt(document.getElementById("columnCount").value),
        startRowChar: document.getElementById("startChar").value.trim().toUpperCase(),
    };
    try {
        await seatApi.createBulk(data);
        Swal.fire("Tạo hàng loạt thành công!", "", "success");
        loadSeats();
    } catch (err) {
        Swal.fire("Lỗi khi tạo hàng loạt!", err.message, "error");
    }
});

// ======================= 7️⃣ FORM CẬP NHẬT HÀNG LOẠT =======================
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
    try {
        await seatApi.bulkUpdateRow(data);
        Swal.fire("Cập nhật hàng loạt thành công!", "", "success");
        loadSeats();
        if (diagramAuditoriumSelect.value) renderSeatDiagram(diagramAuditoriumSelect.value);
    } catch (err) {
        Swal.fire("Lỗi cập nhật hàng loạt!", err.message, "error");
    }
});

// ======================= 8️⃣ SỰ KIỆN LIÊN KẾT =======================
[diagramBranchSelect, singleBranchSelect, bulkBranchSelect, updateBranchSelect].forEach(sel => {
    sel.addEventListener("change", (e) => {
        updateAuditoriumOptions(e.target, e.target.value); // ✅ Gửi branchId = value (số)
    });
});

diagramAuditoriumSelect.addEventListener("change", (e) => {
    renderSeatDiagram(e.target.value);
});

loadButton.addEventListener("click", () => loadSeats());

// ======================= 9️⃣ KHỞI TẠO =======================
await loadBranches();
await loadSeatTypes();
await loadSeats();
