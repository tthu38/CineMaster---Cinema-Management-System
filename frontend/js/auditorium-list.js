import { auditoriumApi } from "./api/auditoriumApi.js";
import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";

import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

// --- DOM Elements ---
const auditoriumsBody = document.getElementById("auditoriums-body");
const filterBranchSelect = document.getElementById("filterBranchID");
const loadButton = document.getElementById("load-auditoriums");
const paginationFooter = document.getElementById("pagination-footer");

// --- Biến trạng thái ---
let allAuditoriumsData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

// ========== 1️⃣ TẢI CHI NHÁNH VÀO DROPDOWN ==========
async function loadBranchesForFilter() {
    try {
        const res = await branchApi.getAllActive();
        if (!res || res.length === 0) {
            filterBranchSelect.innerHTML = `<option value="">Không có chi nhánh hoạt động</option>`;
            return;
        }

        filterBranchSelect.innerHTML = `<option value="">--- Tất Cả Chi Nhánh ---</option>`;
        res.forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.id || b.branchId;
            opt.textContent = b.branchName;
            filterBranchSelect.appendChild(opt);
        });
    } catch (err) {
        console.error("Lỗi khi tải chi nhánh:", err);
        filterBranchSelect.innerHTML = `<option value="">Lỗi tải chi nhánh</option>`;
    }
}

// ========== 2️⃣ HIỂN THỊ DANH SÁCH PHÒNG CHIẾU ==========
function displayAuditoriums(page = 1) {
    auditoriumsBody.innerHTML = "";
    paginationFooter.innerHTML = "";

    if (!allAuditoriumsData || allAuditoriumsData.length === 0) {
        auditoriumsBody.innerHTML = `
            <tr><td colspan="5" class="text-center" style="color:var(--muted)">
            Không có phòng chiếu nào hoạt động.</td></tr>`;
        return;
    }

    const totalItems = allAuditoriumsData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
    currentPage = Math.min(page, totalPages);

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const currentItems = allAuditoriumsData.slice(start, end);

    currentItems.forEach(a => {
        const row = auditoriumsBody.insertRow();
        row.insertCell(0).textContent = a.auditoriumID || a.id;
        row.insertCell(1).textContent = a.name || "N/A";
        row.insertCell(2).textContent = a.branchName || "N/A";
        row.insertCell(3).textContent = a.type || "Không rõ";
        row.insertCell(4).textContent = a.capacity ?? "—";
    });

    renderPagination(totalPages);
}

// ========== 3️⃣ PHÂN TRANG ==========
function renderPagination(totalPages) {
    paginationFooter.innerHTML = "";
    if (totalPages <= 1) return;

    const createPageItem = (label, page, disabled = false, active = false) => `
        <li class="page-item ${disabled ? "disabled" : ""} ${active ? "active" : ""}">
            <a class="page-link" href="#" onclick="loadPage(${page}); return false;">${label}</a>
        </li>`;

    paginationFooter.innerHTML += createPageItem("«", currentPage - 1, currentPage === 1);

    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, currentPage + 2);
    for (let i = start; i <= end; i++) {
        paginationFooter.innerHTML += createPageItem(i, i, false, i === currentPage);
    }

    paginationFooter.innerHTML += createPageItem("»", currentPage + 1, currentPage === totalPages);
}

// helper cho pagination click
window.loadPage = (page) => displayAuditoriums(page);

// ========== 4️⃣ LOAD AUDITORIUMS ==========
async function loadAuditoriums() {
    auditoriumsBody.innerHTML = `
        <tr><td colspan="5" class="text-center" style="color:var(--muted)">
        Đang tải danh sách...</td></tr>`;

    const branchId = filterBranchSelect.value;
    try {
        let res;
        if (branchId) {
            res = await auditoriumApi.getActiveByBranch(branchId);
        } else {
            res = await auditoriumApi.getAllActive();
        }

        if (!res) throw new Error("Không thể kết nối máy chủ");
        allAuditoriumsData = Array.isArray(res) ? res : res.result || [];
        displayAuditoriums(1);
    } catch (err) {
        console.error("Lỗi khi tải danh sách phòng chiếu:", err);
        Swal.fire("Lỗi", err.message || "Không thể tải dữ liệu phòng chiếu", "error");
        auditoriumsBody.innerHTML = `
            <tr><td colspan="5" class="text-center" style="color:var(--red)">
            Lỗi kết nối: ${err.message}</td></tr>`;
    }
}

// ========== 5️⃣ INIT ==========
async function init() {
    if (!requireAuth()) return;
    await loadBranchesForFilter();
    await loadAuditoriums();
}

// ========== 6️⃣ SỰ KIỆN ==========
loadButton.addEventListener("click", loadAuditoriums);
filterBranchSelect.addEventListener("change", loadAuditoriums);
document.addEventListener("DOMContentLoaded", init);
