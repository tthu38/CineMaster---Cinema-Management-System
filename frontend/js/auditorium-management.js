import { auditoriumApi } from "./api/auditoriumApi.js";
import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

// ========== DOM ELEMENTS ==========
let auditoriumForm, auditoriumsBody, loadButton, formTitle,
    submitBtn, cancelBtn, auditoriumIdField, paginationControls,
    branchSelect, filterBranchSelect;

// ========== STATE ==========
let allAuditoriumsData = [];
let isBranchesLoaded = false;
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

// ========== LOAD BRANCHES ==========
async function loadBranches() {
    if (!branchSelect || !filterBranchSelect) {
        console.warn("⚠️ Không tìm thấy select chi nhánh trong DOM.");
        return;
    }

    if (isBranchesLoaded) return;
    branchSelect.innerHTML = `<option>Đang tải chi nhánh...</option>`;
    filterBranchSelect.innerHTML = `<option>Đang tải...</option>`;

    try {
        const branches = await branchApi.getAll();
        if (!branches?.length) {
            branchSelect.innerHTML = `<option>Không có chi nhánh</option>`;
            filterBranchSelect.innerHTML = `<option>Không có chi nhánh</option>`;
            return;
        }

        branchSelect.innerHTML = `<option value="" disabled selected hidden>--- Chọn Chi Nhánh ---</option>`;
        filterBranchSelect.innerHTML = `<option value="">--- Tất Cả Chi Nhánh ---</option>`;

        branches.forEach(b => {
            const label = b.isActive ? b.branchName : `${b.branchName} (Đã đóng ⚠️)`;
            const value = b.id ?? b.branchID ?? b.branchId;
            const opt1 = new Option(label, value);
            const opt2 = new Option(label, value);
            if (!b.isActive) opt1.style.color = "#ff9999";
            branchSelect.appendChild(opt1);
            filterBranchSelect.appendChild(opt2);
        });
        isBranchesLoaded = true;
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
        branchSelect.innerHTML = `<option>Lỗi tải chi nhánh</option>`;
        filterBranchSelect.innerHTML = `<option>Lỗi tải chi nhánh</option>`;
    }
}

// ========== LOAD AUDITORIUMS ==========
async function loadAuditoriums() {
    await loadBranches();

    if (!auditoriumsBody) return;
    auditoriumsBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Đang tải danh sách...</td></tr>`;
    paginationControls.innerHTML = "";

    try {
        const branchId = filterBranchSelect?.value || "";
        const res = branchId ? await auditoriumApi.getByBranch(branchId) : await auditoriumApi.getAll();
        allAuditoriumsData = res || [];
        displayAuditoriums(1);
    } catch (err) {
        console.error("❌ Lỗi khi tải phòng chiếu:", err);
        Swal.fire("Lỗi", "Không thể tải danh sách phòng chiếu.", "error");
        auditoriumsBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Lỗi tải dữ liệu</td></tr>`;
    }
}

// ========== HIỂN THỊ ==========
function displayAuditoriums(page = 1) {
    auditoriumsBody.innerHTML = "";
    if (!allAuditoriumsData.length) {
        auditoriumsBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Không có dữ liệu.</td></tr>`;
        paginationControls.innerHTML = "";
        return;
    }

    const totalPages = Math.ceil(allAuditoriumsData.length / ITEMS_PER_PAGE);
    currentPage = Math.min(Math.max(1, page), totalPages);
    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const data = allAuditoriumsData.slice(start, start + ITEMS_PER_PAGE);

    data.forEach(a => {
        const row = auditoriumsBody.insertRow();
        row.insertCell(0).textContent = a.auditoriumID || a.id;
        row.insertCell(1).textContent = a.name;
        row.insertCell(2).textContent = a.branchName;
        row.insertCell(3).textContent = a.type;
        row.insertCell(4).textContent = a.capacity;
        row.insertCell(5).innerHTML = a.isActive
            ? `<span class="text-success fw-bold">Hoạt động</span>`
            : `<span class="text-danger">Đã đóng</span>`;
        const actionCell = row.insertCell(6);
        actionCell.append(
            createButton("Sửa", "btn-warning me-2", () => populateFormForUpdate(a)),
            createButton(a.isActive ? "Đóng" : "Mở lại", a.isActive ? "btn-danger" : "btn-info",
                () => toggleAuditoriumStatus(a.auditoriumID, !a.isActive))
        );
    });
    renderPaginationControls(totalPages);
}

function createButton(label, cls, onClick) {
    const btn = document.createElement("button");
    btn.textContent = label; btn.className = `btn btn-sm ${cls}`; btn.onclick = onClick;
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
        a.href = "#"; a.className = "page-link"; a.textContent = i;
        a.onclick = e => { e.preventDefault(); displayAuditoriums(i); };
        li.appendChild(a); ul.appendChild(li);
    }
    paginationControls.appendChild(ul);
}

// ========== FORM SUBMIT ==========
async function handleFormSubmission(e) {
    e.preventDefault();
    const data = {
        name: document.getElementById("auditoriumName").value.trim(),
        capacity: parseInt(document.getElementById("capacity").value),
        type: document.getElementById("type").value,
        branchId: parseInt(branchSelect.value)
    };
    if (!data.branchId || isNaN(data.branchId)) {
        Swal.fire("Cảnh báo", "Vui lòng chọn chi nhánh hợp lệ!", "warning");
        return;
    }
    const id = auditoriumIdField.value;
    try {
        if (id) await auditoriumApi.update(id, data);
        else await auditoriumApi.create(data);
        Swal.fire("Thành công!", id ? "Đã cập nhật!" : "Đã tạo mới!", "success");
        resetForm(); loadAuditoriums();
    } catch (err) {
        Swal.fire("Lỗi", "Không thể lưu dữ liệu.", "error");
        console.error(err);
    }
}

async function toggleAuditoriumStatus(id, newStatus) {
    const actionText = newStatus ? "mở lại" : "đóng";
    const confirm = await Swal.fire({
        title: `Xác nhận ${actionText}?`,
        text: `Bạn có chắc muốn ${actionText} phòng chiếu ID ${id}?`,
        icon: newStatus ? "info" : "warning",
        showCancelButton: true,
        confirmButtonText: "Xác nhận",
        cancelButtonText: "Hủy"
    });
    if (!confirm.isConfirmed) return;
    try {
        if (newStatus) await auditoriumApi.activate(id);
        else await auditoriumApi.deactivate(id);
        Swal.fire("Thành công!", `Phòng chiếu đã ${actionText}.`, "success");
        loadAuditoriums();
    } catch {
        Swal.fire("Lỗi", "Không thể thay đổi trạng thái.", "error");
    }
}

function populateFormForUpdate(a) {
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập Nhật Phòng Chiếu (ID: ${a.auditoriumID})`;
    submitBtn.textContent = "Lưu Cập Nhật";
    cancelBtn.style.display = "inline-block";
    auditoriumIdField.value = a.auditoriumID;
    document.getElementById("auditoriumName").value = a.name;
    document.getElementById("capacity").value = a.capacity;
    document.getElementById("type").value = a.type;
    branchSelect.value = a.branchId ?? a.branch?.id;
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function resetForm() {
    auditoriumForm.reset(); auditoriumIdField.value = "";
    formTitle.innerHTML = `<i class="fa-solid fa-plus me-2"></i> Thêm Phòng Chiếu Mới`;
    submitBtn.textContent = "Tạo Phòng Chiếu"; cancelBtn.style.display = "none";
}

// ========== INIT ==========
document.addEventListener("DOMContentLoaded", () => {
    auditoriumForm = document.getElementById("auditorium-form");
    auditoriumsBody = document.getElementById("auditoriums-body");
    loadButton = document.getElementById("load-auditoriums");
    formTitle = document.getElementById("form-title");
    submitBtn = document.getElementById("submit-btn");
    cancelBtn = document.getElementById("cancel-btn");
    auditoriumIdField = document.getElementById("auditoriumID");
    paginationControls = document.getElementById("pagination-controls");
    branchSelect = document.getElementById("branchID");
    filterBranchSelect = document.getElementById("filterBranchID");

    if (!requireAuth()) return;
    if (auditoriumForm) auditoriumForm.addEventListener("submit", handleFormSubmission);
    if (cancelBtn) cancelBtn.addEventListener("click", resetForm);
    if (loadButton) loadButton.addEventListener("click", loadAuditoriums);
    if (filterBranchSelect) filterBranchSelect.addEventListener("change", loadAuditoriums);
    loadAuditoriums();
});
