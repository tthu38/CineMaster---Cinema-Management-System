import { branchApi } from "./api/branchApi.js";
import { requireAuth } from "./api/config.js";
import { accountApi } from "./api/accountApi.js";

let deleteModal, restoreModal;
let deleteNameEl, restoreNameEl;
let confirmDeleteBtn, confirmRestoreBtn;

const table = document.getElementById("accountTable");
const pagination = document.getElementById("pagination");
const branchSelect = document.getElementById("branchFilter");

let currentPage = 0;
const pageSize = 10;
let currentDeleteId = null;
let currentRestoreId = null;
let currentRoleId = null;
let currentKeyword = "";
let currentBranchId = null;
let initialized = false;

let currentRole = null;
let managerBranchId = null;

// ========================= SEARCH =========================
document.getElementById("searchInput").addEventListener("input", e => {
    currentKeyword = e.target.value.trim();
    loadAccounts(0);
});

// ========================= BRANCH FILTER =========================
branchSelect.addEventListener("change", e => {
    const val = e.target.value;
    currentBranchId = val === "" || val === "undefined" ? null : Number(val);
    console.log("🔍 Branch filter:", currentBranchId);
    loadAccounts(0);
});

// ========================= LOAD ACCOUNTS =========================
async function loadAccounts(page = 0) {
    table.innerHTML = `<tr><td colspan="9" class="text-center">Đang tải...</td></tr>`;
    try {
        let branchFilter = currentBranchId;

        const viewingStaff = currentRoleId && Number(currentRoleId) === 2;
        if (viewingStaff && (currentRole === "Manager" || currentRole === "Staff")) {
            branchFilter = managerBranchId;
        }

        const res = await accountApi.getAllPaged(
            page, pageSize, currentRoleId, branchFilter, currentKeyword
        );

        renderTable(res.content);
        renderPagination(res);
        currentPage = res.page;
    } catch (err) {
        console.error(" Error loading accounts:", err);
        table.innerHTML = `<tr><td colspan="9" class="text-center text-danger">Không thể tải danh sách account</td></tr>`;
    }
}

// ========================= LOAD BRANCHES =========================
async function loadBranches() {
    try {
        const branches = await branchApi.getAll();
        branchSelect.innerHTML = `<option value="">Tất cả chi nhánh</option>`;

        branches.forEach(b => {
            const branchValue = b.branchID ?? b.id ?? b.branchId;
            branchSelect.innerHTML += `<option value="${branchValue}">${b.branchName}</option>`;
        });
    } catch (err) {
        console.error("Error loading branches:", err);
        branchSelect.innerHTML = `<option value="">(Lỗi tải chi nhánh)</option>`;
    }
}

// ========================= RENDER TABLE =========================
function renderTable(accounts = []) {
    table.innerHTML = "";
    if (accounts.length === 0) {
        table.innerHTML = `<tr><td colspan="9" class="text-center">Chưa có account nào</td></tr>`;
        return;
    }

    accounts.forEach(acc => {
        const profileImg = acc.avatarUrl
            ? `<img src="${acc.avatarUrl.startsWith("http") ? acc.avatarUrl : "http://localhost:8080" + acc.avatarUrl}" class="profile-img">`
            : `<span class="text-muted">No Image</span>`;

        const statusDot = `<span class="status-dot ${acc.isActive ? "status-active" : "status-inactive"}"
                                title="${acc.isActive ? "Đang hoạt động" : "Đã vô hiệu hóa"}"></span>`;

        let actionButtons = "";

        // 🧑‍💼 ADMIN
        if (currentRole === "Admin") {
            actionButtons = acc.isActive
                ? `
            <a href="updateUser.html?id=${acc.accountID}" class="btn btn-sm btn-warning me-1">Sửa</a>
            <button class="btn btn-sm btn-danger btn-delete" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Xóa</button>`
                : `<button class="btn btn-sm btn-success btn-restore" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Khôi phục</button>`;
        }
        // 🧑‍💼 MANAGER
        else if (currentRole === "Manager") {
            if (acc.roleName === "Staff") {
                actionButtons = acc.isActive
                    ? `
                <a href="updateUser.html?id=${acc.accountID}" class="btn btn-sm btn-warning me-1">Sửa</a>
                <button class="btn btn-sm btn-danger btn-delete" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Xóa</button>`
                    : `<button class="btn btn-sm btn-success btn-restore" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Khôi phục</button>`;
            } else {
                actionButtons = `<span class="text-muted">—</span>`;
            }
        }
        // 👷 STAFF
        else if (currentRole === "Staff") {
            actionButtons = `<span class="text-muted">—</span>`;
        }

        table.innerHTML += `
            <tr data-id="${acc.accountID}">
              <td>${acc.accountID}</td>
              <td>${profileImg}</td>
              <td>${acc.email}</td>
              <td>${acc.fullName || ""}</td>
              <td>${acc.phoneNumber || ""}</td>
              <td>${statusDot}</td>
              <td>${acc.roleName || ""}</td>
              <td>${acc.branchName || ""}</td>
              <td>${actionButtons}</td>
            </tr>
        `;
    });

    attachRowEvents();
}

// ========================= ATTACH EVENTS =========================
function attachRowEvents() {
    document.querySelectorAll(".btn-delete").forEach(btn => {
        btn.addEventListener("click", () => {
            currentDeleteId = btn.dataset.id;
            deleteNameEl.textContent = btn.dataset.name;
            deleteModal.show();
        });
    });

    document.querySelectorAll(".btn-restore").forEach(btn => {
        btn.addEventListener("click", () => {
            currentRestoreId = btn.dataset.id;
            restoreNameEl.textContent = btn.dataset.name;
            restoreModal.show();
        });
    });
}

//========================= PAGINATION =========================
function renderPagination(pageData) {
    pagination.innerHTML = "";
    if (!pageData || pageData.totalPages <= 1) return;

    const makeButton = (page, label, disabled = false, active = false) => {
        const btn = document.createElement("button");
        btn.className = `btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1`;
        btn.innerHTML = label;
        btn.disabled = disabled;
        btn.addEventListener("click", () => loadAccounts(page));
        return btn;
    };

    // Previous
    pagination.appendChild(makeButton(pageData.page - 1, "&laquo;", pageData.page === 0));

    // Page numbers
    for (let i = 0; i < pageData.totalPages; i++) {
        pagination.appendChild(makeButton(i, i + 1, false, i === pageData.page));
    }

    // Next
    pagination.appendChild(makeButton(pageData.page + 1, "&raquo;", pageData.page === pageData.totalPages - 1));
}


// ========================= DELETE / RESTORE =========================
function attachModalActions() {
    confirmDeleteBtn.addEventListener("click", async () => {
        if (!currentDeleteId) return;
        try {
            await accountApi.remove(currentDeleteId);
            deleteModal.hide();
            updateRowStatus(currentDeleteId, false);
        } catch (err) {
            console.error("Error deleting account:", err);
            alert("Vô hiệu hóa thất bại!");
        }
    });

    confirmRestoreBtn.addEventListener("click", async () => {
        if (!currentRestoreId) return;
        try {
            await accountApi.restore(currentRestoreId);
            restoreModal.hide();
            updateRowStatus(currentRestoreId, true);
        } catch (err) {
            console.error("Error restoring account:", err);
            alert("Khôi phục thất bại!");
        }
    });
}

// ========================= CẬP NHẬT DÒNG TRONG BẢNG =========================
function updateRowStatus(accountID, isActive) {
    const row = table.querySelector(`tr[data-id="${accountID}"]`);
    if (!row) return;

    const dotCell = row.children[5];
    dotCell.innerHTML = `<span class="status-dot ${isActive ? "status-active" : "status-inactive"}"
                             title="${isActive ? "Đang hoạt động" : "Đã vô hiệu hóa"}"></span>`;

    const actionCell = row.children[8];
    actionCell.innerHTML = isActive
        ? `<a href="updateUser.html?id=${accountID}" class="btn btn-sm btn-warning me-1">Sửa</a>
           <button class="btn btn-sm btn-danger btn-delete" data-id="${accountID}">Xóa</button>`
        : `<button class="btn btn-sm btn-success btn-restore" data-id="${accountID}">Khôi phục</button>`;

    attachRowEvents();
}

// ========================= ROLE FILTER BUTTONS =========================
document.querySelectorAll(".filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        currentRoleId = btn.dataset.role || null;
        loadAccounts(0);
    });
});

// ========================= INIT =========================
async function init() {
    if (initialized) return;
    initialized = true;

    if (!requireAuth()) return;

    currentRole = localStorage.getItem("role");
    managerBranchId = localStorage.getItem("branchId");

    console.log(" Role:", currentRole, "Branch:", managerBranchId);

    if (currentRole === "Manager") {
        branchSelect.parentElement.style.display = "none";
        currentBranchId = Number(managerBranchId);
    }
    else if (currentRole === "Staff") {
        branchSelect.parentElement.style.display = "none";
        currentBranchId = Number(managerBranchId);
    }
    else {
        await loadBranches();
    }

    await loadAccounts();
}

// ========================= WAIT DOM THEN START =========================
document.addEventListener("DOMContentLoaded", () => {
    // Modal chắc chắn tồn tại
    const deleteEl = document.getElementById("deleteAccountModal");
    const restoreEl = document.getElementById("restoreAccountModal");

    if (deleteEl && restoreEl) {
        deleteModal = new bootstrap.Modal(deleteEl);
        restoreModal = new bootstrap.Modal(restoreEl);
        deleteNameEl = document.getElementById("deleteAccountName");
        restoreNameEl = document.getElementById("restoreAccountName");
        confirmDeleteBtn = document.getElementById("confirmDeleteBtn");
        confirmRestoreBtn = document.getElementById("confirmRestoreBtn");
        attachModalActions();
    }

    init();
});
