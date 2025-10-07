import { auditoriumApi, branchApi, requireAuth } from "./api.js";

const auditoriumForm = document.getElementById("auditorium-form");
const auditoriumsBody = document.getElementById("auditoriums-body");
const loadButton = document.getElementById("load-auditoriums");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");
const auditoriumIdField = document.getElementById("auditoriumID");
const paginationControls = document.getElementById("pagination-controls");

const branchSelect = document.getElementById("branchID");
const filterBranchSelect = document.getElementById("filterBranchID");

let isBranchesLoaded = false;
let allAuditoriumsData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

// ====== LOAD BRANCHES ======
async function loadBranches() {
    if (isBranchesLoaded) return;

    branchSelect.innerHTML = `<option value="">Đang tải chi nhánh...</option>`;
    filterBranchSelect.innerHTML = `<option value="">Đang tải...</option>`;

    try {
        const branches = await branchApi.getAllBranches();
        if (!branches || branches.length === 0) {
            branchSelect.innerHTML = `<option value="">Không có chi nhánh</option>`;
            filterBranchSelect.innerHTML = `<option value="">Không có chi nhánh</option>`;
            return;
        }

        branchSelect.innerHTML = `<option value="" disabled selected hidden>--- Chọn Chi Nhánh ---</option>`;
        filterBranchSelect.innerHTML = `<option value="">--- Tất Cả Chi Nhánh ---</option>`;

        branches.forEach(b => {
            const label = b.isActive ? b.branchName : `${b.branchName} (Đã đóng ⚠️)`;
            // ✅ Hỗ trợ cả id / branchID / branchId
            const value = b.id ?? b.branchID ?? b.branchId;

            const option1 = new Option(label, value);
            const option2 = new Option(label, value);

            if (!b.isActive) option1.style.color = "#ff9999";
            branchSelect.appendChild(option1);
            filterBranchSelect.appendChild(option2);
        });

        isBranchesLoaded = true;
    } catch (err) {
        console.error("Lỗi tải chi nhánh:", err);
        branchSelect.innerHTML = `<option value="">Lỗi tải chi nhánh</option>`;
    }
}

// ====== LOAD AUDITORIUMS ======
async function loadAuditoriums() {
    await loadBranches();

    auditoriumsBody.innerHTML =
        `<tr><td colspan="7" class="text-center text-muted">Đang tải danh sách...</td></tr>`;
    paginationControls.innerHTML = "";

    try {
        const branchId = filterBranchSelect.value;
        const res = branchId
            ? await auditoriumApi.getByBranch(branchId)
            : await auditoriumApi.getAll();

        allAuditoriumsData = res || [];
        displayAuditoriums(1);
    } catch (err) {
        console.error("Lỗi khi tải phòng chiếu:", err);
        Swal.fire("Lỗi", "Không thể tải danh sách phòng chiếu.", "error");
        auditoriumsBody.innerHTML =
            `<tr><td colspan="7" class="text-center text-danger">Lỗi tải dữ liệu</td></tr>`;
    }
}

// ====== HIỂN THỊ DỮ LIỆU + PHÂN TRANG ======
function displayAuditoriums(page = 1) {
    auditoriumsBody.innerHTML = "";

    if (allAuditoriumsData.length === 0) {
        auditoriumsBody.innerHTML =
            `<tr><td colspan="7" class="text-center text-muted">Không tìm thấy phòng chiếu nào.</td></tr>`;
        paginationControls.innerHTML = "";
        return;
    }

    const totalItems = allAuditoriumsData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
    currentPage = Math.min(Math.max(1, page), totalPages);

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const data = allAuditoriumsData.slice(start, end);

    data.forEach(a => {
        const row = auditoriumsBody.insertRow();
        row.insertCell(0).textContent = a.auditoriumID;
        row.insertCell(1).textContent = a.name;
        row.insertCell(2).textContent = a.branchName;
        row.insertCell(3).textContent = a.type;
        row.insertCell(4).textContent = a.capacity;

        const statusCell = row.insertCell(5);
        const statusText = a.isActive ? "Hoạt động" : "Đã đóng";
        const statusClass = a.isActive ? "text-success fw-bold" : "text-danger";
        statusCell.innerHTML = `<span class="${statusClass}">${statusText}</span>`;

        const actionCell = row.insertCell(6);
        const editBtn = createButton("Sửa", "btn-warning edit-btn me-2", () => populateFormForUpdate(a));
        const toggleBtn = createButton(
            a.isActive ? "Đóng" : "Mở lại",
            a.isActive ? "btn-danger" : "btn-info",
            () => toggleAuditoriumStatus(a.auditoriumID, !a.isActive)
        );
        actionCell.append(editBtn, toggleBtn);
    });

    renderPaginationControls(totalPages);
}

function createButton(label, cls, onClick) {
    const btn = document.createElement("button");
    btn.textContent = label;
    btn.className = `btn btn-sm ${cls}`;
    btn.onclick = onClick;
    return btn;
}

function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    const ul = document.createElement("ul");
    ul.className = "pagination pagination-sm";

    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement("li");
        li.className = `page-item ${i === currentPage ? "active" : ""}`;
        const a = document.createElement("a");
        a.href = "#";
        a.className = "page-link";
        a.textContent = i;
        a.onclick = e => {
            e.preventDefault();
            displayAuditoriums(i);
        };
        li.appendChild(a);
        ul.appendChild(li);
    }
    paginationControls.appendChild(ul);
}

// ====== TẠO / CẬP NHẬT ======
async function handleFormSubmission(e) {
    e.preventDefault();

    const data = {
        name: document.getElementById("auditoriumName").value.trim(),
        capacity: parseInt(document.getElementById("capacity").value),
        type: document.getElementById("type").value,
        branchId: parseInt(branchSelect.value), // ✅ đúng theo DTO
    };

    console.log("📤 Payload gửi lên:", data); // log kiểm tra

    if (!data.branchId || isNaN(data.branchId)) {
        Swal.fire("Cảnh báo", "Vui lòng chọn chi nhánh hợp lệ!", "warning");
        return;
    }

    const id = auditoriumIdField.value;
    const isUpdate = !!id;

    try {
        if (isUpdate) await auditoriumApi.update(id, data);
        else await auditoriumApi.create(data);

        Swal.fire("Thành công!", `Phòng chiếu đã được ${isUpdate ? "cập nhật" : "tạo"}!`, "success");
        resetForm();
        loadAuditoriums();
    } catch (err) {
        console.error("Lỗi lưu phòng chiếu:", err);
        Swal.fire("Thất bại", err.message || "Không thể lưu dữ liệu.", "error");
    }
}

// ====== ĐÓNG / MỞ LẠI ======
async function toggleAuditoriumStatus(id, newStatus) {
    const actionText = newStatus ? "mở lại" : "đóng";

    const confirm = await Swal.fire({
        title: `Xác nhận ${actionText} phòng chiếu?`,
        text: `Bạn có chắc muốn ${actionText} phòng chiếu ID ${id}?`,
        icon: newStatus ? "info" : "warning",
        showCancelButton: true,
        confirmButtonText: "Xác nhận",
        cancelButtonText: "Hủy",
        reverseButtons: true,
    });

    if (!confirm.isConfirmed) return;

    try {
        if (newStatus) await auditoriumApi.activate(id);
        else await auditoriumApi.deactivate(id);

        Swal.fire("Thành công!", `Phòng chiếu ID ${id} đã được ${actionText}.`, "success");
        loadAuditoriums();
    } catch (err) {
        console.error("Lỗi cập nhật trạng thái:", err);
        Swal.fire("Lỗi", "Không thể thay đổi trạng thái phòng chiếu.", "error");
    }
}

// ====== FORM CẬP NHẬT / RESET ======
function populateFormForUpdate(a) {
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập Nhật Phòng Chiếu (ID: ${a.auditoriumID})`;
    submitBtn.innerHTML = `<i class="fa-solid fa-floppy-disk me-2"></i> Lưu Cập Nhật`;
    cancelBtn.style.display = "inline-block";

    auditoriumIdField.value = a.auditoriumID;
    document.getElementById("auditoriumName").value = a.name;
    document.getElementById("capacity").value = a.capacity;
    document.getElementById("type").value = a.type;
    branchSelect.value = a.branchID ?? a.branchId ?? a.id;
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function resetForm() {
    auditoriumForm.reset();
    auditoriumIdField.value = "";
    formTitle.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Thêm Phòng Chiếu Mới`;
    submitBtn.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Tạo Phòng Chiếu`;
    cancelBtn.style.display = "none";
}

// ====== KHỞI TẠO ======
function init() {
    if (!requireAuth()) return;
    loadAuditoriums();
}

auditoriumForm.addEventListener("submit", handleFormSubmission);
cancelBtn.addEventListener("click", resetForm);
loadButton.addEventListener("click", loadAuditoriums);
filterBranchSelect.addEventListener("change", loadAuditoriums);

document.addEventListener("DOMContentLoaded", init);
