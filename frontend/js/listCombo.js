import { requireAuth } from "./api/config.js";
import { comboApi } from "./api/comboApi.js";
import { branchApi } from "./api/branchApi.js";

let dataTable;
let allCombos = [];
let currentBranch = "";
let currentAvailable = "";
let currentKeyword = "";

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
        // 🔒 Manager chỉ được xem combo của chi nhánh mình
        document.getElementById("branchFilter").style.display = "none"; // ẩn dropdown chi nhánh
        await loadCombosByBranch(branchId);
    } else {
        // Admin load tất cả
        await loadBranches();
        await loadCombos();
    }

    // Filter buttons
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentAvailable = btn.dataset.available;
            handleFilters();
        });
    });

    // Search input
    const searchInput = document.getElementById("searchInput");
    searchInput.addEventListener("input", () => {
        currentKeyword = searchInput.value.trim().toLowerCase();
        handleFilters();
    });

    // Branch filter (Admin only)
    const branchSelect = document.getElementById("branchFilter");
    if (branchSelect) branchSelect.addEventListener("change", handleFilters);
}

/* ==================== LOAD BRANCHES ==================== */
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

/* ==================== LOAD COMBOS ==================== */
async function loadCombos() {
    try {
        const data = await comboApi.getAll();
        allCombos = data || [];
        renderTable(allCombos);
    } catch (err) {
        console.error("❌ Lỗi khi tải combo:", err);
    }
}

/* ==================== LOAD COMBOS THEO CHI NHÁNH (MANAGER) ==================== */
async function loadCombosByBranch(branchId) {
    try {
        const data = await comboApi.getByBranch(branchId);
        allCombos = data || [];
        renderTable(allCombos);
    } catch (err) {
        console.error("❌ Lỗi khi tải combo chi nhánh:", err);
    }
}

/* ==================== FILTER ==================== */
function handleFilters() {
    const branchId = document.getElementById("branchFilter")?.value || "";
    currentBranch = branchId;

    let filtered = [...allCombos];
    if (branchId) filtered = filtered.filter(c => String(c.branchId) === String(branchId));
    if (currentAvailable === "1") filtered = filtered.filter(c => c.available === true);
    else if (currentAvailable === "0") filtered = filtered.filter(c => c.available === false);

    if (currentKeyword) {
        const keyword = currentKeyword.replace(/[^\w\s]/g, "");
        filtered = filtered.filter(c => {
            const text = `${c.nameCombo} ${c.descriptionCombo || ""} ${c.items || ""}`.toLowerCase();
            const price = (c.price || "").toString().toLowerCase();
            return text.includes(keyword) || price.includes(keyword);
        });
    }

    renderTable(filtered);
}

window.deleteCombo = function (id) {
    showConfirm("Bạn có chắc muốn ẩn combo này không?", async () => {
        try {
            await comboApi.delete(id);
            showToast("✅ Combo đã được ẩn!");
            await refreshAfterAction();
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
            await refreshAfterAction();
        } catch (err) {
            console.error("❌ Lỗi khi khôi phục combo:", err);
            showToast("⚠️ Lỗi khi khôi phục combo!", "error");
        }
    });
};

async function refreshAfterAction() {
    const role = localStorage.getItem("role");
    const branchId = localStorage.getItem("branchId");
    if (role === "Manager") await loadCombosByBranch(branchId);
    else await loadCombos();
    handleFilters();
}

/* ==================== TABLE RENDER ==================== */
function renderTable(data) {
    if (dataTable) dataTable.destroy();

    const tbody = document.querySelector("#comboTable tbody");
    tbody.innerHTML = "";

    if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Không có combo nào</td></tr>`;
        return;
    }

    data.forEach(c => {
        const img = c.imageURL
            ? `<img src="${c.imageURL.startsWith("http") ? c.imageURL : `http://localhost:8080${c.imageURL}`}" width="60" class="rounded">`
            : `<span class="text-muted">No Image</span>`;

        tbody.innerHTML += `
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
          ${
            c.available
                ? `<button class="btn btn-danger btn-sm" onclick="deleteCombo(${c.id})">Ẩn</button>`
                : `<button class="btn btn-success btn-sm" onclick="restoreCombo(${c.id})">Khôi phục</button>`
        }
        </td>
      </tr>`;
    });

    dataTable = new DataTable("#comboTable", {
        paging: true,
        searching: false,
        info: false,
        pageLength: 10,
        lengthChange: false,
    });
}
