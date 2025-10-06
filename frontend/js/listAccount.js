import { accountApi, branchApi, requireAuth } from './api.js';

const table = document.getElementById("accountTable");
const pagination = document.getElementById("pagination");
const branchSelect = document.getElementById("branchFilter");

const deleteModal = new bootstrap.Modal(document.getElementById("deleteAccountModal"));
const deleteNameEl = document.getElementById("deleteAccountName");
const confirmDeleteBtn = document.getElementById("confirmDeleteBtn");

const restoreModal = new bootstrap.Modal(document.getElementById("restoreAccountModal"));
const restoreNameEl = document.getElementById("restoreAccountName");
const confirmRestoreBtn = document.getElementById("confirmRestoreBtn");

let currentPage = 0;
const pageSize = 10;
let currentDeleteId = null;
let currentRestoreId = null;
let currentRoleId = null;
let currentKeyword = "";
let currentBranchId = null;

// ===== Event search =====
document.getElementById("searchInput").addEventListener("input", e => {
    currentKeyword = e.target.value;
    loadAccounts(0);
});

// ===== Event chọn branch =====
branchSelect.addEventListener("change", e => {
    currentBranchId = e.target.value || null;
    loadAccounts(0);
});

// ===== Load account =====
async function loadAccounts(page = 0) {
    table.innerHTML = `<tr><td colspan="9" class="text-center">Đang tải...</td></tr>`;
    try {
        const res = await accountApi.getAllPaged(
            page,
            pageSize,
            currentRoleId,
            currentBranchId,
            currentKeyword
        );
        renderTable(res.content);
        renderPagination(res);
        currentPage = res.page;
    } catch (err) {
        console.error("Error loading accounts:", err);
        table.innerHTML = `<tr><td colspan="9" class="text-center text-danger">Không thể tải danh sách account</td></tr>`;
    }
}

// ===== Load branch list =====
async function loadBranches() {
    try {
        const branches = await branchApi.getAll(); // 👈 gọi API chung
        branchSelect.innerHTML = `<option value="">Tất cả chi nhánh</option>`;
        branches.forEach(b => {
            branchSelect.innerHTML += `<option value="${b.id}">${b.branchName}</option>`;
        });
    } catch (err) {
        console.error("❌ Error loading branches:", err);
        branchSelect.innerHTML = `<option value="">(Lỗi tải chi nhánh)</option>`;
    }
}

// 🎯 Event khi chọn chi nhánh
branchSelect.addEventListener("change", e => {
    currentBranchId = e.target.value || null;
    loadAccounts(0);
});


// ===== Render bảng account =====
function renderTable(accounts) {
    table.innerHTML = "";
    if (!accounts || accounts.length === 0) {
        table.innerHTML = `<tr><td colspan="9" class="text-center">Chưa có account nào</td></tr>`;
        return;
    }

    accounts.forEach(acc => {
        const profileImg = acc.avatarUrl
            ? `<img src="${acc.avatarUrl.startsWith("http") ? acc.avatarUrl : "http://localhost:8080" + acc.avatarUrl}" alt="Profile" class="profile-img">`
            : 'No Image';

        const isActiveDot = acc.isActive
            ? `<span class="status-dot status-active"></span>`
            : `<span class="status-dot status-inactive"></span>`;

        const actionButtons = acc.isActive
            ? `
                <a href="updateUser.html?id=${acc.accountID}" class="btn btn-sm btn-warning">Sửa</a>
                <button class="btn btn-sm btn-danger btn-delete" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Xóa</button>
              `
            : `
                <button class="btn btn-sm btn-success btn-restore" data-id="${acc.accountID}" data-name="${acc.fullName || acc.email}">Khôi phục</button>
              `;

        table.innerHTML += `
            <tr>
              <td>${acc.accountID}</td>
              <td>${profileImg}</td>
              <td>${acc.email}</td>
              <td>${acc.fullName || ""}</td>
              <td>${acc.phoneNumber || ""}</td>
              <td>${isActiveDot}</td>
              <td>${acc.roleName || ""}</td>
              <td>${acc.branchName || ""}</td>
              <td>${actionButtons}</td>
            </tr>`;
    });

    // Gắn event cho nút Xóa / Khôi phục
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

// ===== Render phân trang =====
function renderPagination(pageData) {
    pagination.innerHTML = "";
    if (pageData.totalPages <= 1) return;

    // Prev
    pagination.innerHTML += `
      <button class="btn btn-sm btn-secondary me-1"
              ${pageData.page === 0 ? "disabled" : ""}
              onclick="loadAccounts(${pageData.page - 1})">&laquo;</button>
    `;

    for (let i = 0; i < pageData.totalPages; i++) {
        pagination.innerHTML += `
          <button class="btn btn-sm ${i === pageData.page ? 'btn-primary' : 'btn-secondary'} me-1"
                  onclick="loadAccounts(${i})">${i + 1}</button>
        `;
    }

    // Next
    pagination.innerHTML += `
      <button class="btn btn-sm btn-secondary"
              ${pageData.page === pageData.totalPages - 1 ? "disabled" : ""}
              onclick="loadAccounts(${pageData.page + 1})">&raquo;</button>
    `;
}

// ===== Delete / Restore =====
confirmDeleteBtn.addEventListener("click", async () => {
    if (!currentDeleteId) return;
    try {
        await accountApi.remove(currentDeleteId);
        deleteModal.hide();
        loadAccounts(currentPage);
    } catch (err) {
        console.error("Error deleting account:", err);
        alert("❌ Vô hiệu hóa thất bại!");
    }
});

confirmRestoreBtn.addEventListener("click", async () => {
    if (!currentRestoreId) return;
    try {
        await accountApi.restore(currentRestoreId);
        restoreModal.hide();
        loadAccounts(currentPage);
    } catch (err) {
        console.error("Error restoring account:", err);
        alert("❌ Khôi phục thất bại!");
    }
});

// ===== Gắn sự kiện cho filter button =====
document.querySelectorAll(".filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        currentRoleId = btn.dataset.role || null;
        console.log("🔍 Filter roleId:", currentRoleId); // log ra roleId
        loadAccounts(0);
    });
});

// ===== Init =====
async function init() {
    if (!requireAuth()) return;
    await loadBranches();
    loadAccounts();
}


window.loadAccounts = loadAccounts;
init();
