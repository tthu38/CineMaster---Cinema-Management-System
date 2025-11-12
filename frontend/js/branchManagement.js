import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

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

function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    const createBtn = (page, label, disabled = false, active = false) => `
  <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
          ${disabled ? "disabled" : ""}
          onclick="goToPage(${page})">${label}</button>
`;

    paginationControls.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 1);
    for (let i = 1; i <= totalPages; i++) {
        paginationControls.innerHTML += createBtn(i, i, false, i === currentPage);
    }
    paginationControls.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages);

}

function displayBranches(page = 1) {
    branchesBody.innerHTML = "";
    paginationControls.innerHTML = "";
    const hideActionColumn = window.readOnlyMode;

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
        if (!hideActionColumn) {
            const actCell = row.insertCell(8);

            const editLink = document.createElement("a");
            editLink.href = `updateBranch.html?id=${b.branchId}`;
            editLink.className = "btn btn-warning btn-sm me-2";
            editLink.textContent = "Sửa";

            const toggleBtn = document.createElement("button");
            toggleBtn.className = `btn btn-sm ${active ? "btn-danger" : "btn-success"}`;
            toggleBtn.textContent = active ? "Đóng" : "Mở lại";
            toggleBtn.onclick = () => handleStatusChange(b.branchId, active);
            actCell.style.display = "flex";
            actCell.style.justifyContent = "center";
            actCell.style.gap = "8px";
            actCell.append(editLink, toggleBtn);

        }
    });
}

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
        if (isActive) {
            await branchApi.delete(id);
        } else {
            await branchApi.restore(id);
        }

        Swal.fire("Thành công!", `Chi nhánh đã được ${actionText}.`, "success");
        await loadBranches();
    } catch (err) {
        console.error(err);
        Swal.fire("Lỗi", `Không thể ${actionText} chi nhánh: ${err.message}`, "error");
    }
}

async function handleFormSubmission(e) {
    e.preventDefault();

    const id = branchIdField.value;
    const isUpdate = id !== "";

    const branchName = document.getElementById("branchName").value.trim();
    const address = document.getElementById("address").value.trim();
    let phoneValue = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const openTime = document.getElementById("openTime").value;
    const closeTime = document.getElementById("closeTime").value;
    const managerValue = document.getElementById("managerId").value.trim();

    phoneValue = phoneValue.replace(/\D/g, "");

    if (phoneValue.startsWith("84") && phoneValue.length === 11) {
        phoneValue = "0" + phoneValue.slice(2);
    }
    if (!/^[0-9]{10}$/.test(phoneValue)) {
        Swal.fire("Số điện thoại không hợp lệ", "Số điện thoại phải gồm đúng 10 chữ số (VD: 0987654321).", "error");
        return;
    }
    const managerId = managerValue && managerValue !== "0" ? parseInt(managerValue) : null;
    const payload = {
        branchName,
        address,
        phone: phoneValue,
        email,
        openTime,
        closeTime,
        managerId
    };

    try {
        if (isUpdate) {
            await branchApi.update(id, payload);
        } else {
            await branchApi.create(payload);
        }

        Swal.fire({
            icon: "success",
            title: "Thành công!",
            text: `Chi nhánh đã được ${isUpdate ? "cập nhật" : "tạo mới"} thành công.`,
            timer: 1500,
            showConfirmButton: false
        });

        resetForm();
        await loadBranches();

    } catch (err) {
        console.error("Lỗi:", err);
        Swal.fire("Thất bại", err.message || "Không thể lưu chi nhánh", "error");
    }
}

function populateFormForUpdate(b) {
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


function resetForm() {
    branchForm.reset();
    branchIdField.value = "";
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Tạo Chi Nhánh';
    cancelBtn.style.display = "none";
}


async function init() {
    if (!requireAuth()) return;

    const role = localStorage.getItem("role");
    const isAdmin = role === "Admin";
    const isManager = role === "Manager";
    const isStaff = role === "Staff";

    if (isManager || isStaff) {
        console.log(" Quyền xem danh sách (Manager/Staff)");
        // Ẩn form CRUD
        const form = document.getElementById("branch-form");
        if (form) form.style.display = "none";

        // Ẩn tiêu đề cột “Hành động”
        const ths = document.querySelectorAll("th");
        ths.forEach(th => {
            if (th.textContent.trim() === "Hành động") {
                th.style.display = "none";
            }
        });

        window.readOnlyMode = true;
    } else {
        window.readOnlyMode = false;
    }

    await loadBranches();
}

branchForm.addEventListener("submit", handleFormSubmission);
loadButton.addEventListener("click", loadBranches);
cancelBtn.addEventListener("click", resetForm);
document.addEventListener("DOMContentLoaded", init);
window.goToPage = (page) => displayBranches(page);
