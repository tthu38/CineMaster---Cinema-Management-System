import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

let allBranchesData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;
window.readOnlyMode = false;

const branchForm = document.getElementById("branch-form");
const branchesBody = document.getElementById("branches-body");
const loadButton = document.getElementById("load-branches");
const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");
const branchIdField = document.getElementById("branchId");
const paginationControls = document.getElementById("pagination-controls");
const searchInput = document.getElementById("searchBranch");


searchInput.addEventListener("input", async (e) => {
    const keyword = e.target.value.trim();

    if (keyword.length === 0) {
        currentPage = 1;       // <<< RESET TRANG VỀ ĐÚNG TRẠNG 1
        await loadBranches();
        return;
    }

    try {
        const results = await branchApi.search(keyword);
        allBranchesData = results;
        currentPage = 1;
        displayBranches(1);
    } catch (err) {
        console.error("Lỗi search:", err);
    }
});

window.goToPage = (page) => displayBranches(page);

function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    const btn = (page, label, disabled = false, active = false) => `
        <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
            ${disabled ? "disabled" : ""}
            onclick="goToPage(${page})">${label}</button>
    `;

    paginationControls.innerHTML += btn(currentPage - 1, "&laquo;", currentPage === 1);

    for (let i = 1; i <= totalPages; i++) {
        paginationControls.innerHTML += btn(i, i, false, i === currentPage);
    }

    paginationControls.innerHTML += btn(currentPage + 1, "&raquo;", currentPage === totalPages);
}

async function loadBranches() {
    branchesBody.innerHTML = `
        <tr><td colspan="9" class="text-center text-muted py-3">Đang tải danh sách...</td></tr>`;

    paginationControls.innerHTML = "";

    try {
        const res = await branchApi.getAll();
        allBranchesData = Array.isArray(res) ? res : res.result || [];

        displayBranches(1);

    } catch (err) {
        console.error("Lỗi tải danh sách:", err);
        branchesBody.innerHTML = `
            <tr><td colspan="9" class="text-center text-danger py-3">
            Lỗi tải dữ liệu: ${err.message}</td></tr>`;
    }
}

function displayBranches(page = 1) {
    branchesBody.innerHTML = "";
    paginationControls.innerHTML = "";

    if (!allBranchesData.length) {
        branchesBody.innerHTML = `
            <tr><td colspan="9" class="text-center text-muted py-3">
            Không có chi nhánh nào.</td></tr>`;
        return;
    }

    currentPage = page;
    const totalPages = Math.ceil(allBranchesData.length / ITEMS_PER_PAGE);

    const start = (page - 1) * ITEMS_PER_PAGE;
    const branchesToDisplay = allBranchesData.slice(start, start + ITEMS_PER_PAGE);

    renderPaginationControls(totalPages);

    branchesToDisplay.forEach((b) => {
        const row = branchesBody.insertRow();
        row.insertCell(0).textContent = b.branchId;
        row.insertCell(1).textContent = b.branchName;
        row.insertCell(2).textContent = b.address;
        row.insertCell(3).textContent = b.phone;
        row.insertCell(4).textContent = b.email;
        row.insertCell(5).textContent = `${b.openTime} - ${b.closeTime}`;
        const mgrCell = row.insertCell(6);
        mgrCell.classList.add("d-none");

        row.insertCell(7).innerHTML =
            `<span class="badge bg-${b.isActive ? "success" : "danger"}">
                ${b.isActive ? "Hoạt động" : "Đã đóng"}
            </span>`;

        const actCell = row.insertCell(8);

        if (window.readOnlyMode) {
            actCell.innerHTML = `<span class="text-muted">Không có quyền</span>`;
            return;
        }

        const editBtn = document.createElement("button");
        editBtn.className = "btn btn-warning btn-sm me-2";
        editBtn.textContent = "Sửa";
        editBtn.onclick = () => populateFormForUpdate(b);

        const toggleBtn = document.createElement("button");
        toggleBtn.className = `btn btn-sm ${b.isActive ? "btn-danger" : "btn-success"}`;
        toggleBtn.textContent = b.isActive ? "Đóng" : "Mở lại";
        toggleBtn.onclick = () => handleStatusChange(b.branchId, b.isActive);

        actCell.append(editBtn, toggleBtn);
    });
}

async function handleStatusChange(id, isActive) {
    const actionText = isActive ? "đóng tạm" : "mở lại";

    const confirm = await Swal.fire({
        icon: "warning",
        title: `${isActive ? "Đóng" : "Mở lại"} chi nhánh?`,
        text: `Bạn có chắc muốn ${actionText} chi nhánh ID ${id}?`,
        showCancelButton: true,
        confirmButtonText: "Xác nhận",
        cancelButtonText: "Hủy"
    });

    if (!confirm.isConfirmed) return;

    try {
        if (isActive) await branchApi.delete(id);
        else await branchApi.restore(id);

        Swal.fire("Thành công!", `Chi nhánh đã được ${actionText}.`, "success");
        loadBranches();

    } catch (err) {
        Swal.fire("Lỗi", err.message, "error");
    }
}

async function handleFormSubmission(e) {
    e.preventDefault();

    const id = branchIdField.value;
    const isUpdate = id !== "";

    const name = document.getElementById("branchName").value.trim();
    const address = document.getElementById("address").value.trim();
    let phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const openTime = document.getElementById("openTime").value;
    const closeTime = document.getElementById("closeTime").value;
    const mgrValue = document.getElementById("managerId").value.trim();

    phone = phone.replace(/\D/g, "");
    if (!/^\d{10}$/.test(phone)) {
        Swal.fire("Lỗi", "Số điện thoại phải gồm 10 số.", "error");
        return;
    }

    const payload = {
        branchName: name,
        address,
        phone,
        email,
        openTime,
        closeTime
    };


    try {
        if (isUpdate) await branchApi.update(id, payload);
        else await branchApi.create(payload);

        Swal.fire("Thành công!", isUpdate ? "Đã cập nhật!" : "Đã tạo mới!", "success");

        resetForm();
        loadBranches();

    } catch (err) {
        Swal.fire("Lỗi", err.message || "Không thể lưu chi nhánh", "error");
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

    // Scroll mượt giống Auditorium
    window.scrollTo({
        top: branchForm.offsetTop - 50,
        behavior: "smooth"
    });
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

    // Read-only mode
    if (!isAdmin) {
        window.readOnlyMode = true;
        branchForm.style.display = "none";

        document.querySelectorAll("th").forEach((th) => {
            if (th.textContent.trim() === "Hành động") th.style.display = "none";
        });
    }

    loadBranches();
}

branchForm.addEventListener("submit", handleFormSubmission);
cancelBtn.addEventListener("click", resetForm);
loadButton.addEventListener("click", loadBranches);
document.addEventListener("DOMContentLoaded", init);
