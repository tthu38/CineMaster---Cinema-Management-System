import { requireAuth } from "./api/config.js";
import { comboApi } from "./api/comboApi.js";
import { branchApi } from "./api/branchApi.js";


const tableBody = document.querySelector("#comboTable tbody");
const pagination = document.getElementById("pagination");
let allCombos = [];
let filteredCombos = [];
let currentBranch = "";
let currentAvailable = "";
let currentKeyword = "";
let currentPage = 0;
const pageSize = 10;


/* ==================== TOAST & CONFIRM ==================== */
function showToast(message, type = "success") {
    const bg = type === "error" ? "bg-danger" : "bg-success";
    const toastEl = document.createElement("div");
    toastEl.className = `toast align-items-center text-white ${bg} border-0 mb-2`;
    toastEl.innerHTML = `
   <div class="d-flex">
     <div class="toast-body fw-semibold">${message}</div>
     <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
   </div>`;
    document.getElementById("toastContainer").appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 2500 });
    toast.show();
    toastEl.addEventListener("hidden.bs.toast", () => toastEl.remove());
}


function showConfirm(message, onConfirm) {
    document.getElementById("confirmMessage").textContent = message;
    const modal = new bootstrap.Modal(document.getElementById("confirmModal"));
    modal.show();
    const okBtn = document.getElementById("confirmOkBtn");


    const handleOk = () => {
        modal.hide();
        okBtn.removeEventListener("click", handleOk);
        if (onConfirm) onConfirm();
    };
    okBtn.addEventListener("click", handleOk);
}


/* ==================== INIT ==================== */
document.addEventListener("DOMContentLoaded", init);


async function init() {
    if (!requireAuth()) return;


    const role = localStorage.getItem("role");
    const branchId = localStorage.getItem("branchId");


    if (role === "Manager") {
        document.getElementById("branchFilter").style.display = "none";
        await loadCombosByBranch(branchId);
    } else {
        await loadBranches();
        await loadCombos();
    }


    // Bộ lọc trạng thái
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentAvailable = btn.dataset.available;
            handleFilters(0);
        });
    });


    // Tìm kiếm
    document.getElementById("searchInput").addEventListener("input", e => {
        currentKeyword = e.target.value.trim().toLowerCase();
        handleFilters(0);
    });


    // Lọc chi nhánh
    const branchSelect = document.getElementById("branchFilter");
    if (branchSelect) branchSelect.addEventListener("change", () => handleFilters(0));
}


/* ==================== LOAD DATA ==================== */
async function loadBranches() {
    try {
        const data = await branchApi.getAll();
        const branchSelect = document.getElementById("branchFilter");
        branchSelect.innerHTML = `<option value="">Tất cả Chi Nhánh</option>`;
        data.forEach(branch => {
            branchSelect.innerHTML += `<option value="${branch.id}">${branch.branchName || branch.name}</option>`;
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải chi nhánh:", err);
    }
}


async function loadCombos() {
    try {
        const data = await comboApi.getAll();
        allCombos = data || [];
        handleFilters(0);
    } catch (err) {
        console.error("❌ Lỗi khi tải combo:", err);
    }
}


async function loadCombosByBranch(branchId) {
    try {
        const data = await comboApi.getByBranch(branchId);
        allCombos = data || [];
        handleFilters(0);
    } catch (err) {
        console.error("❌ Lỗi khi tải combo chi nhánh:", err);
    }
}


/* ==================== FILTER & PAGINATION ==================== */
function handleFilters(page = 0) {
    const branchId = document.getElementById("branchFilter")?.value || "";
    currentBranch = branchId;


    filteredCombos = [...allCombos];


    if (branchId) filteredCombos = filteredCombos.filter(c => String(c.branchId) === String(branchId));
    if (currentAvailable === "1") filteredCombos = filteredCombos.filter(c => c.available);
    else if (currentAvailable === "0") filteredCombos = filteredCombos.filter(c => !c.available);


    if (currentKeyword) {
        const keyword = currentKeyword.replace(/[^\w\s]/g, "");
        filteredCombos = filteredCombos.filter(c => {
            const text = `${c.nameCombo} ${c.descriptionCombo || ""} ${c.items || ""}`.toLowerCase();
            const price = (c.price || "").toString().toLowerCase();
            return text.includes(keyword) || price.includes(keyword);
        });
    }


    renderPage(page);
}


function renderPage(page) {
    currentPage = page;
    const start = page * pageSize;
    const end = start + pageSize;
    const pageData = filteredCombos.slice(start, end);
    renderTable(pageData);
    renderPagination();
}


/* ==================== TABLE ==================== */
function renderTable(data) {
    tableBody.innerHTML = "";


    if (!data.length) {
        tableBody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Không có combo nào</td></tr>`;
        return;
    }


    data.forEach(c => {
        const img = c.imageURL
            ? `<img src="${c.imageURL.startsWith("http") ? c.imageURL : `http://localhost:8080${c.imageURL}`}" width="60" class="rounded">`
            : `<span class="text-muted">No Image</span>`;


        tableBody.innerHTML += `
     <tr>
       <td>${img}</td>
       <td>${c.nameCombo}</td>
       <td>${c.price.toLocaleString("vi-VN")} đ</td>
       <td title="${c.descriptionCombo || ""}">${c.descriptionCombo || ""}</td>
       <td title="${c.items || ""}">${c.items || ""}</td>
       <td>
         <span class="badge ${c.available ? "bg-success" : "bg-secondary"}">
           ${c.available ? "Có sẵn" : "Hết hàng"}
         </span>
       </td>
       <td>${c.branchName || ""}</td>
       <td>
         <a href="updateCombo.html?id=${c.id}" class="btn btn-warning btn-sm me-2">Sửa</a>
         ${c.available
            ? `<button class="btn btn-danger btn-sm" onclick="deleteCombo(${c.id})">Ẩn</button>`
            : `<button class="btn btn-success btn-sm" onclick="restoreCombo(${c.id})">Khôi phục</button>`}
       </td>
     </tr>`;
    });
}


/* ==================== PAGINATION ==================== */
function renderPagination() {
    pagination.innerHTML = "";
    const totalPages = Math.ceil(filteredCombos.length / pageSize);
    if (totalPages <= 1) return;


    const createBtn = (page, label, disabled = false, active = false) => `
   <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
           ${disabled ? "disabled" : ""}
           onclick="goToPage(${page})">${label}</button>
 `;


    pagination.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 0);
    for (let i = 0; i < totalPages; i++) {
        pagination.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    }
    pagination.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages - 1);
}


window.goToPage = page => renderPage(page);


/* ==================== DELETE / RESTORE ==================== */
window.deleteCombo = function (id) {
    showConfirm("Bạn có chắc muốn ẩn combo này không?", async () => {
        try {
            await comboApi.delete(id);
            showToast("✅ Combo đã được ẩn!");
            await reloadAfterAction();
        } catch (err) {
            console.error("❌ Lỗi khi xóa combo:", err);
            showToast("⚠️ Lỗi khi xóa combo!", "error");
        }
    });
};


window.restoreCombo = function (id) {
    showConfirm("Khôi phục combo này?", async () => {
        try {
            await comboApi.restore(id);
            showToast("♻️ Combo đã được khôi phục!");
            await reloadAfterAction();
        } catch (err) {
            console.error("❌ Lỗi khi khôi phục combo:", err);
            showToast("⚠️ Lỗi khi khôi phục combo!", "error");
        }
    });
};


async function reloadAfterAction() {
    const role = localStorage.getItem("role");
    const branchId = localStorage.getItem("branchId");
    if (role === "Manager") await loadCombosByBranch(branchId);
    else await loadCombos();
}
