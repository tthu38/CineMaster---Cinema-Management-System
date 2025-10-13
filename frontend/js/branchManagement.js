import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

// --- PHÂN TRANG ---
let allBranchesData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

const branchForm = document.getElementById("branch-form");
const branchesBody = document.getElementById("branches-body");
const loadButton = document.getElementById("load-branches");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");
const branchIdField = document.getElementById("branchId");
const paginationControls = document.getElementById("pagination-controls");

// --- PHÂN TRANG ---
function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    const ul = document.createElement("ul");
    ul.className = "pagination pagination-sm";

    const createPageLink = (text, pageNumber, isDisabled = false, isCurrent = false) => {
        const li = document.createElement("li");
        li.className = `page-item ${isDisabled ? "disabled" : ""} ${isCurrent ? "active" : ""}`;
        const a = document.createElement("a");
        a.href = "#";
        a.className = "page-link";
        a.textContent = text;
        if (!isDisabled) {
            a.onclick = (e) => {
                e.preventDefault();
                displayBranches(pageNumber);
            };
        }
        li.appendChild(a);
        return li;
    };

    ul.appendChild(createPageLink("«", currentPage - 1, currentPage === 1));

    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageLink(i, i, false, i === currentPage));
    }

    ul.appendChild(createPageLink("»", currentPage + 1, currentPage === totalPages));
    paginationControls.appendChild(ul);
}

// --- HIỂN THỊ DANH SÁCH ---
function displayBranches(page = 1) {
    branchesBody.innerHTML = "";
    paginationControls.innerHTML = "";

    if (!allBranchesData || allBranchesData.length === 0) {
        branchesBody.innerHTML = `
            <tr><td colspan="9" class="text-center" style="color:var(--muted)">
            Chưa có chi nhánh nào.</td></tr>`;
        return;
    }

    currentPage = page;
    const totalItems = allBranchesData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const branchesToDisplay = allBranchesData.slice(start, end);
    renderPaginationControls(totalPages);

    branchesToDisplay.forEach((b) => {
        const row = branchesBody.insertRow();
        row.insertCell(0).textContent = b.branchId;
        row.insertCell(1).textContent = b.branchName;
        row.insertCell(2).textContent = b.address;
        row.insertCell(3).textContent = b.phone;
        row.insertCell(4).textContent = b.email;
        row.insertCell(5).textContent = `${b.openTime} - ${b.closeTime}`;
        const mgrDisplay = b.managerId
            ? `${b.managerId} (${b.managerName || "N/A"})`
            : "Chưa gán";
        row.insertCell(6).textContent = mgrDisplay;

        // Trạng thái
        const stCell = row.insertCell(7);
        const active = b.isActive;
        stCell.innerHTML = `<span class="badge bg-${active ? "success" : "danger"}">
            ${active ? "Hoạt động" : "Đã đóng"}
        </span>`;

        // Hành động
        const actCell = row.insertCell(8);

        const editBtn = document.createElement("button");
        editBtn.className = "btn btn-warning btn-sm me-2 mb-1";
        editBtn.textContent = "Sửa";
        editBtn.onclick = () => populateFormForUpdate(b);
        actCell.appendChild(editBtn);

        const toggleBtn = document.createElement("button");
        toggleBtn.className = `btn btn-sm ${active ? "btn-secondary delete-btn" : "btn-primary"}`;
        toggleBtn.textContent = active ? "Đóng" : "Mở lại";
        toggleBtn.onclick = () => handleStatusChange(b.branchId, active);
        actCell.appendChild(toggleBtn);
    });
}

// --- LOAD DANH SÁCH ---
async function loadBranches() {
    branchesBody.innerHTML = `
        <tr><td colspan="9" class="text-center" style="color:var(--muted)">
        Đang tải danh sách...</td></tr>`;
    paginationControls.innerHTML = "";

    try {
        const res = await branchApi.getAll();
        if (!res) throw new Error("Không thể kết nối máy chủ");
        allBranchesData = Array.isArray(res) ? res : res.result || [];
        displayBranches(1);
    } catch (err) {
        console.error("Lỗi khi tải danh sách:", err);
        Swal.fire("Lỗi Kết Nối", err.message, "error");
        branchesBody.innerHTML = `
            <tr><td colspan="9" class="text-center" style="color:var(--red)">
            Lỗi kết nối: ${err.message}</td></tr>`;
    }
}

// --- ĐÓNG / MỞ LẠI CHI NHÁNH --- (Soft Delete / Restore)
async function handleStatusChange(id, isActive) {
    const actionText = isActive ? "đóng (xoá tạm)" : "mở lại";
    const confirm = await Swal.fire({
        title: isActive ? "Đóng chi nhánh?" : "Mở lại chi nhánh?",
        text: `Bạn có chắc muốn ${actionText} chi nhánh ID ${id}?`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Xác nhận",
        cancelButtonText: "Hủy",
        reverseButtons: true,
    });
    if (!confirm.isConfirmed) return;

    try {
        // ✅ Gọi đúng hàm API tương ứng
        if (isActive) {
            await branchApi.delete(id);   // Gọi DELETE /branches/{id}
        } else {
            await branchApi.restore(id);  // Gọi PUT /branches/{id}/restore
        }

        Swal.fire("Thành công!", `Chi nhánh đã được ${actionText}.`, "success");
        await loadBranches();
    } catch (err) {
        console.error(err);
        Swal.fire("Lỗi", `Không thể ${actionText} chi nhánh: ${err.message}`, "error");
    }
}


// --- TẠO / CẬP NHẬT ---
async function handleFormSubmission(e) {
    e.preventDefault();

    const id = branchIdField.value;
    const isUpdate = id !== "";
    const payload = {
        branchName: document.getElementById("branchName").value,
        address: document.getElementById("address").value,
        phone: document.getElementById("phone").value,
        email: document.getElementById("email").value,
        openTime: document.getElementById("openTime").value,
        closeTime: document.getElementById("closeTime").value,
        managerId: document.getElementById("managerId").value
            ? parseInt(document.getElementById("managerId").value)
            : null,
    };

    try {
        if (isUpdate) await branchApi.update(id, payload);
        else await branchApi.create(payload);

        Swal.fire(
            "Thành công!",
            `Chi nhánh đã được ${isUpdate ? "cập nhật" : "tạo mới"}.`,
            "success"
        );
        resetForm();
        loadBranches();
    } catch (err) {
        console.error("Lỗi:", err);
        Swal.fire("Thất bại", err.message || "Không thể lưu chi nhánh", "error");
    }
}

// --- NẠP FORM ---
function populateFormForUpdate(b) {
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập nhật Chi nhánh (ID: ${b.branchId})`;
    submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i> Lưu Cập Nhật';
    cancelBtn.style.display = "inline-block";

    branchIdField.value = b.branchId;
    document.getElementById("branchName").value = b.branchName;
    document.getElementById("address").value = b.address;
    document.getElementById("phone").value = b.phone;
    document.getElementById("email").value = b.email;
    document.getElementById("openTime").value = b.openTime;
    document.getElementById("closeTime").value = b.closeTime;
    document.getElementById("managerId").value = b.managerId || "";
    window.scrollTo({ top: 0, behavior: "smooth" });
}

// --- RESET FORM ---
function resetForm() {
    branchForm.reset();
    branchIdField.value = "";
    formTitle.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Thêm Chi Nhánh Mới';
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Tạo Chi Nhánh';
    cancelBtn.style.display = "none";
}

// --- INIT (BẮT BUỘC CHECK LOGIN) ---
async function init() {
    if (!requireAuth()) return; // ✅ kiểm tra token đăng nhập
    await loadBranches();
}

// --- SỰ KIỆN ---
branchForm.addEventListener("submit", handleFormSubmission);
loadButton.addEventListener("click", loadBranches);
cancelBtn.addEventListener("click", resetForm);
document.addEventListener("DOMContentLoaded", init);
