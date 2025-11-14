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
        const seats = await seatApi.getByAuditorium(auditoriumId);
        if (!seats.length) {
            seatDiagram.innerHTML = `<p class="text-center text-muted">Chưa có ghế trong phòng chiếu này.</p>`;
            return;
        }


        // Gom theo dãy
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


                // ✅ Ghép row + number để hiển thị đúng
                const seatLabel = `${s.seatRow || ""}${s.seatNumber || ""}`;
                box.textContent = seatLabel;
                box.title = `${seatLabel} - ${s.typeName} (${s.status})`;


                // ✅ Khi click vào ghế trong sơ đồ
                box.addEventListener("click", async () => {
                    selectedSeatId = s.seatID;
                    document.querySelectorAll(".seat-box").forEach(el => el.classList.remove("seat-selected"));
                    box.classList.add("seat-selected");

                    // Giữ nguyên logic load form (để sửa)
                    loadSeatToForm(s);
                    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Đang chỉnh sửa: ${seatLabel}`;
                    submitBtn.innerHTML = `<i class="fa-solid fa-check me-2"></i> Cập Nhật Ghế`;
                    cancelBtn.style.display = "inline-block";

                    const currentStatus = (s.status || "").toUpperCase();

                    // ======================== POPUP HÀNH ĐỘNG ========================
                    if (currentStatus === "AVAILABLE") {
                        const confirm = await Swal.fire({
                            title: `Đánh dấu ghế ${seatLabel} là "Broken"?`,
                            text: "Ghế sẽ tạm thời không khả dụng cho khách đặt chỗ.",
                            icon: "warning",
                            showCancelButton: true,
                            confirmButtonText: "Xác nhận",
                            cancelButtonText: "Hủy",
                            confirmButtonColor: "#d33",
                        });
                        if (!confirm.isConfirmed) return;

                        try {
                            const updateData = {
                                ...s,
                                status: "Broken",
                                auditoriumID: s.auditoriumID,
                                typeID: s.typeID,
                                seatRow: s.seatRow,
                                seatNumber: s.seatNumber,
                                columnNumber: s.columnNumber,
                            };
                            await seatApi.update(s.seatID, updateData);
                            Swal.fire("✅ Đã đánh dấu ghế hỏng!", "", "success");

                            // Cập nhật lại sơ đồ + bảng
                            const currentAuditorium = diagramAuditoriumSelect.value;
                            await renderSeatDiagram(currentAuditorium);
                            await loadSeatsByAuditorium(currentAuditorium);
                        } catch (err) {
                            console.error("❌ Lỗi khi cập nhật ghế:", err);
                            Swal.fire("Lỗi!", err.message || "Không thể cập nhật ghế.", "error");
                        }

                    } else if (currentStatus === "BROKEN") {
                        const confirm = await Swal.fire({
                            title: `Khôi phục ghế ${seatLabel}?`,
                            text: "Ghế sẽ hoạt động trở lại để bán vé.",
                            icon: "question",
                            showCancelButton: true,
                            confirmButtonText: "Khôi phục",
                            cancelButtonText: "Hủy",
                            confirmButtonColor: "#3085d6",
                        });
                        if (!confirm.isConfirmed) return;

                        try {
                            const updateData = {
                                ...s,
                                status: "Available",
                                auditoriumID: s.auditoriumID,
                                typeID: s.typeID,
                                seatRow: s.seatRow,
                                seatNumber: s.seatNumber,
                                columnNumber: s.columnNumber,
                            };
                            await seatApi.update(s.seatID, updateData);
                            Swal.fire("✅ Đã khôi phục ghế!", "", "success");

                            const currentAuditorium = diagramAuditoriumSelect.value;
                            await renderSeatDiagram(currentAuditorium);
                            await loadSeatsByAuditorium(currentAuditorium);
                        } catch (err) {
                            console.error("❌ Lỗi khi khôi phục ghế:", err);
                            Swal.fire("Lỗi!", err.message || "Không thể cập nhật ghế.", "error");
                        }
                    }
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
        const seatRow = s.seatRow || "";
        const seatNumber = s.seatNumber || "";
        const status = (s.status || "").toUpperCase();

        // 🎨 Dùng badge kiểu CineMaster
        let statusBadge = "";
        if (status === "AVAILABLE") {
            statusBadge = `<span class="badge rounded-pill bg-success px-3 py-2">Đang hoạt động</span>`;
        } else if (status === "BROKEN") {
            statusBadge = `<span class="badge rounded-pill bg-warning text-dark px-3 py-2">Đang bảo trì</span>`;
        } else if (status === "RESERVED") {
            statusBadge = `<span class="badge rounded-pill bg-secondary px-3 py-2">Đang giữ chỗ</span>`;
        } else {
            statusBadge = `<span class="badge rounded-pill bg-light text-dark px-3 py-2">${s.status}</span>`;
        }

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="text-center">${s.seatID}</td>
            <td class="text-center">${s.auditoriumName || "?"}</td>
            <td class="text-center">${seatRow}</td>
            <td class="text-center">${seatNumber}</td>
            <td class="text-center">${s.typeName}</td>
            <td class="text-center">${statusBadge}</td>
            <td class="text-center">
                ${status !== "BROKEN"
            ? `<button class="btn btn-sm btn-danger btn-delete d-inline-flex align-items-center justify-content-center gap-1" data-id="${s.seatID}">
                        <i class="fa-solid fa-ban"></i> Xóa
                   </button>`
            : `<button class="btn btn-sm btn-success btn-restore d-inline-flex align-items-center justify-content-center gap-1" data-id="${s.seatID}">
                        <i class="fa-solid fa-rotate-left"></i> Khôi phục
                   </button>`}
            </td>`;
        seatsBody.appendChild(tr);
    });

    /* ======================== NÚT XÓA ======================== */
    document.querySelectorAll(".btn-delete").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const id = e.currentTarget.dataset.id;
            const confirm = await Swal.fire({
                title: "Xác nhận xóa ghế?",
                text: "Ghế này sẽ được đánh dấu là 'Broken'.",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Xóa (Broken)",
                cancelButtonText: "Hủy",
                confirmButtonColor: "#d33",
            });
            if (!confirm.isConfirmed) return;

            try {
                const seat = await seatApi.getById(id);
                const updateData = {
                    ...seat,
                    status: "Broken",
                    auditoriumID: seat.auditoriumID,
                    typeID: seat.typeID,
                    seatRow: seat.seatRow,
                    seatNumber: seat.seatNumber,
                    columnNumber: seat.columnNumber,
                };
                await seatApi.update(id, updateData);
                Swal.fire("✅ Đã đánh dấu ghế hỏng!", "", "success");

                // 🔁 Giữ đúng phòng hiện tại
                const currentAuditorium = diagramAuditoriumSelect.value;
                if (currentAuditorium) {
                    await renderSeatDiagram(currentAuditorium);
                    await loadSeatsByAuditorium(currentAuditorium);
                } else await loadSeats();
            } catch (err) {
                console.error("❌ Lỗi khi xóa ghế:", err);
                Swal.fire("Lỗi!", err.message || "Không thể cập nhật ghế.", "error");
            }
        });
    });

    /* ======================== NÚT KHÔI PHỤC ======================== */
    document.querySelectorAll(".btn-restore").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const id = e.currentTarget.dataset.id;
            const confirm = await Swal.fire({
                title: "Khôi phục ghế?",
                text: "Ghế sẽ được đổi lại trạng thái 'Available'.",
                icon: "question",
                showCancelButton: true,
                confirmButtonText: "Khôi phục",
                cancelButtonText: "Hủy",
                confirmButtonColor: "#3085d6",
            });
            if (!confirm.isConfirmed) return;

            try {
                const seat = await seatApi.getById(id);
                const updateData = {
                    ...seat,
                    status: "Available",
                    auditoriumID: seat.auditoriumID,
                    typeID: seat.typeID,
                    seatRow: seat.seatRow,
                    seatNumber: seat.seatNumber,
                    columnNumber: seat.columnNumber,
                };
                await seatApi.update(id, updateData);
                Swal.fire("✅ Đã khôi phục ghế!", "", "success");

                const currentAuditorium = diagramAuditoriumSelect.value;
                if (currentAuditorium) {
                    await renderSeatDiagram(currentAuditorium);
                    await loadSeatsByAuditorium(currentAuditorium);
                } else await loadSeats();
            } catch (err) {
                console.error("❌ Lỗi khi khôi phục ghế:", err);
                Swal.fire("Lỗi!", err.message || "Không thể cập nhật ghế.", "error");
            }
        });
    });
}




function renderPagination(total, currentPage, pageSize) {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";
    const totalPages = Math.ceil(total / pageSize);
    if (totalPages <= 1) return;

    const maxVisible = 5;
    const startPage = Math.max(0, Math.min(currentPage - Math.floor(maxVisible / 2), totalPages - maxVisible));
    const endPage = Math.min(totalPages, startPage + maxVisible);

    const createBtn = (page, label, disabled = false, active = false) => `
        <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
                ${disabled ? "disabled" : ""}
                onclick="goToSeatPage(${page})">${label}</button>
    `;
    pagination.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 0);
    for (let i = startPage; i < endPage; i++) {
        pagination.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    }
    pagination.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages - 1);
}



window.goToSeatPage = (page) => {
    const auditoriumId = diagramAuditoriumSelect?.value;
    if (auditoriumId) loadSeatsByAuditorium(auditoriumId, page);
    else loadSeats(page);
};



// ======================= 5️⃣ LOAD FORM (EDIT) =======================
async function loadSeatToForm(s) {
    document.getElementById("seatID").value = s.seatID;
    document.getElementById("seatRow").value = s.seatRow;
    document.getElementById("columnNumber").value = s.columnNumber;
    document.getElementById("seatNumber").value = s.seatNumber;
    document.getElementById("status").value = s.status;


    if (s.branchID) {
        singleBranchSelect.value = s.branchID;
        await updateAuditoriumOptions(singleBranchSelect, s.branchID);
    }
    if (s.auditoriumID) auditoriumSelect.value = s.auditoriumID;
    if (s.typeID) seatTypeSelect.value = s.typeID;


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
    const seatRow = document.getElementById("seatRow").value.trim().toUpperCase();
    const seatNumber = document.getElementById("seatNumber").value.trim();


    const data = {
        auditoriumID: parseInt(auditoriumSelect.value),
        typeID: parseInt(seatTypeSelect.value),
        seatRow,
        columnNumber: parseInt(document.getElementById("columnNumber").value),
        seatNumber, // chỉ số, không kèm row
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


// ======================= ⛳️ BỔ SUNG NÂNG CẤP (KHÔNG SỬA CODE GỐC) =======================


// ✅ Hàm load ghế theo phòng chiếu (được kế thừa từ bản dưới)
async function loadSeatsByAuditorium(auditoriumId, page = 0, size = 10) {
    try {
        const allSeats = await seatApi.getAll();
        const data = allSeats.filter(s => s.auditoriumID === parseInt(auditoriumId));
        renderSeatTable(data.slice(page * size, (page + 1) * size));
        renderPagination(data.length, page, size);
    } catch (err) {
        console.error("❌ Lỗi tải danh sách ghế theo phòng chiếu:", err);
    }
}

diagramAuditoriumSelect.addEventListener("change", async (e) => {
    const auditoriumId = e.target.value;
    const branchId = diagramBranchSelect.value;

    // ✅ Hiển thị sơ đồ + danh sách ghế theo phòng
    await renderSeatDiagram(auditoriumId);
    await loadSeatsByAuditorium(auditoriumId);

    // ======================= 🔄 Đồng bộ sang các form khác =======================
    try {
        // 1️⃣ Set chi nhánh cho 3 form còn lại
        singleBranchSelect.value = branchId;
        bulkBranchSelect.value = branchId;
        updateBranchSelect.value = branchId;

        // 2️⃣ Cập nhật danh sách phòng chiếu tương ứng
        await updateAuditoriumOptions(singleBranchSelect, branchId);
        await updateAuditoriumOptions(bulkBranchSelect, branchId);
        await updateAuditoriumOptions(updateBranchSelect, branchId);

        // 3️⃣ Set phòng chiếu trùng với đang xem sơ đồ
        auditoriumSelect.value = auditoriumId;
        bulkAuditoriumSelect.value = auditoriumId;
        updateAuditoriumSelect.value = auditoriumId;
    } catch (err) {
        console.error("❌ Lỗi khi đồng bộ form với sơ đồ:", err);
    }
});


loadButton.addEventListener("click", () => {
    const auditoriumId = diagramAuditoriumSelect.value;
    if (!auditoriumId) {
        Swal.fire("Vui lòng chọn Phòng chiếu trước!", "", "info");
        return;
    }
    loadSeatsByAuditorium(auditoriumId);
});
