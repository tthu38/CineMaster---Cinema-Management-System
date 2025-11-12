import { discountApi } from "./api/discountApi.js";
import { membershipLevelApi } from "./api/membershipLevelApi.js";

const tbody = document.getElementById("discountBody");
const pagination = document.getElementById("pagination");
const toastContainer = document.getElementById("toastContainer");
const confirmModalEl = document.getElementById("confirmModal");
const confirmModal = new bootstrap.Modal(confirmModalEl);
const confirmMessage = document.getElementById("confirmMessage");
const confirmOkBtn = document.getElementById("confirmOkBtn");

let allDiscounts = [];
let filteredDiscounts = [];
let currentStatus = "";
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
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 2500 });
    toast.show();
    toastEl.addEventListener("hidden.bs.toast", () => toastEl.remove());
}

function showConfirm(message, onConfirm) {
    confirmMessage.textContent = message;
    confirmModal.show();
    confirmOkBtn.onclick = () => {
        confirmModal.hide();
        onConfirm && onConfirm();
    };
}

/* ==================== INIT ==================== */
document.addEventListener("DOMContentLoaded", async () => {
    await loadDiscounts();

    // Filter buttons
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentStatus = btn.dataset.status;
            handleFilters(0);
        });
    });

    // Search
    document.getElementById("searchInput").addEventListener("input", e => {
        currentKeyword = e.target.value.trim().toLowerCase();
        handleFilters(0);
    });
});

/* ==================== LOAD DATA ==================== */
async function loadDiscounts() {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center p-4">Đang tải dữ liệu...</td></tr>`;
    try {
        const data = await discountApi.getAll();
        allDiscounts = data.result || data || [];
        handleFilters(0);
    } catch (err) {
        console.error("❌ Lỗi tải dữ liệu:", err);
        tbody.innerHTML = `<tr><td colspan="7" class="text-danger p-4">Không thể tải dữ liệu.</td></tr>`;
    }
}

/* ==================== FILTER & PAGINATION ==================== */
function handleFilters(page = 0) {
    filteredDiscounts = [...allDiscounts];

    if (currentStatus)
        filteredDiscounts = filteredDiscounts.filter(d => (d.discountStatus || "").toUpperCase() === currentStatus);

    if (currentKeyword) {
        const keyword = currentKeyword.replace(/[^\w\s]/g, "");
        filteredDiscounts = filteredDiscounts.filter(d =>
            `${d.code} ${d.discountDescription || ""}`.toLowerCase().includes(keyword)
        );
    }

    renderPage(page);
}

function renderPage(page) {
    currentPage = page;
    const start = page * pageSize;
    const end = start + pageSize;
    const pageData = filteredDiscounts.slice(start, end);
    renderTable(pageData);
    renderPagination();
}

/* ==================== RENDER TABLE ==================== */
function renderTable(data) {
    tbody.innerHTML = "";
    if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-muted py-3">Không có mã nào.</td></tr>`;
        return;
    }

    data.forEach(d => {
        const amount = d.percentOff ? `${d.percentOff}%` : `${d.fixedAmount?.toLocaleString("vi-VN")}₫`;
        const start = d.createAt ? new Date(d.createAt).toLocaleDateString("vi-VN") : "-";
        const end = d.expiryDate ? new Date(d.expiryDate).toLocaleDateString("vi-VN") : "-";
        const status = (d.discountStatus || "UNKNOWN").trim().toUpperCase();

        tbody.innerHTML += `
      <tr>
        <td>${d.code}</td>
        <td>${amount}</td>
        <td>${start}</td>
        <td>${end}</td>
        <td>${d.maxUsage ?? "∞"}</td>
        <td><span class="badge ${status === "ACTIVE" ? "bg-success" : status === "EXPIRED" ? "bg-secondary" : "bg-warning"}">${status}</span></td>
        <td>
          <a href="updateDiscount.html?id=${d.discountID}" class="btn btn-warning btn-sm me-2">Sửa</a>
          ${status === "INACTIVE"
            ? `<button class="btn btn-success btn-sm" onclick="restoreDiscount(${d.discountID})">Khôi phục</button>`
            : `<button class="btn btn-danger btn-sm" onclick="deleteDiscount(${d.discountID})">Tạm ngưng</button>`}
        </td>
      </tr>`;
    });
}

/* ==================== PAGINATION ==================== */
function renderPagination() {
    pagination.innerHTML = "";
    const totalPages = Math.ceil(filteredDiscounts.length / pageSize);
    if (totalPages <= 1) return;

    const createBtn = (page, label, disabled = false, active = false) =>
        `<button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
            ${disabled ? "disabled" : ""} onclick="goToPage(${page})">${label}</button>`;

    pagination.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 0);
    for (let i = 0; i < totalPages; i++)
        pagination.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    pagination.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages - 1);
}

window.goToPage = page => renderPage(page);

/* ==================== DELETE / RESTORE ==================== */
window.deleteDiscount = function (id) {
    showConfirm("Bạn có chắc muốn tạm ngưng mã giảm giá này?", async () => {
        try {
            await discountApi.softDelete(id);
            showToast("🟡 Đã tạm ngưng mã giảm giá!");
            await loadDiscounts();
        } catch (err) {
            console.error("❌ Lỗi khi tạm ngưng:", err);
            showToast("⚠️ Lỗi khi tạm ngưng!", "error");
        }
    });
};

window.restoreDiscount = function (id) {
    showConfirm("Khôi phục mã giảm giá này?", async () => {
        try {
            await discountApi.restore(id);
            showToast("♻️ Mã giảm giá đã được khôi phục!");
            await loadDiscounts();
        } catch (err) {
            console.error("❌ Lỗi khi khôi phục:", err);
            showToast("⚠️ Lỗi khi khôi phục!", "error");
        }
    });
};
