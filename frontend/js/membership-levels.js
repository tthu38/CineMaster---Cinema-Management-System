// ================= MEMBERSHIP LEVEL MANAGER =================
import { membershipLevelApi } from './api/membershipLevelApi.js';

const tblBody = document.getElementById("tblBody");
const formEl = document.getElementById("levelForm");
const levelModal = new bootstrap.Modal(document.getElementById("levelModal"));
const btnSave = document.getElementById("btnSave");
const btnCreate = document.getElementById("btnCreate");
const modalTitle = document.getElementById("levelModalTitle");
const pagination = document.getElementById("pagination");

let state = {
    page: 0,
    size: 10,
    sort: "id,ASC",
    totalPages: 1,
    editingId: null,
};
function renderTable(page) {
    const data = page.content || [];
    if (!data.length) {
        tblBody.innerHTML = `
          <tr>
            <td colspan="5" class="text-center text-muted py-4">
              Không có dữ liệu
            </td>
          </tr>`;
        return;
    }

    tblBody.innerHTML = data.map(x => `
      <tr data-id="${x.id}">
        <td>${x.id}</td>
        <td>${x.levelName}</td>
        <td>${x.minPoints} – ${x.maxPoints}</td>
        <td>${x.benefits || ''}</td>
        <td class="text-end">
          <div class="d-inline-flex gap-2 justify-content-end align-items-center flex-nowrap">
            <button class="btn btn-sm btn-warning act-edit" title="Sửa" data-id="${x.id}">
              <i class="fa-solid fa-pen me-1"></i> Sửa
            </button>
            <button class="btn btn-sm btn-danger act-del" title="Xóa" data-id="${x.id}" data-name="${x.levelName}">
              <i class="fa-solid fa-trash me-1"></i> Xóa
            </button>
          </div>
        </td>
      </tr>
    `).join('');

    document.querySelectorAll(".act-edit").forEach(btn => btn.addEventListener("click", onEdit));
    document.querySelectorAll(".act-del").forEach(btn => btn.addEventListener("click", onDelete));
}

async function fetchPage() {
    tblBody.innerHTML = `
      <tr><td colspan="5" class="text-center text-info py-4">Đang tải dữ liệu...</td></tr>`;
    try {
        const page = await membershipLevelApi.list(state.page, state.size, state.sort);
        state.totalPages = page.totalPages;
        renderTable(page);
        renderPagination();
    } catch (err) {
        console.error(err);
        tblBody.innerHTML = `
          <tr><td colspan="5" class="text-center text-danger py-4">Không tải được dữ liệu</td></tr>`;
    }
}
function renderPagination() {
    if (!pagination) return;
    pagination.innerHTML = "";

    const totalPages = state.totalPages;
    const currentPage = state.page;

    // Nếu chỉ có 1 trang → không hiện nút
    if (totalPages <= 1) return;

    // Tạo nút helper
    const createBtn = (page, label, disabled = false, active = false) => `
        <button class="btn btn-sm ${active ? "btn-primary" : "btn-outline-info"} me-1"
            ${disabled ? "disabled" : ""} 
            onclick="goToPage(${page})">${label}</button>
    `;

    pagination.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 0);

    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages, start + 5);
    for (let i = start; i < end; i++) {
        pagination.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    }

    pagination.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages - 1);
}

window.goToPage = (page) => {
    if (page >= 0 && page < state.totalPages) {
        state.page = page;
        fetchPage();
    }
};

async function onEdit(e) {
    const id = e.currentTarget.closest("tr").dataset.id;
    try {
        const data = await membershipLevelApi.get(id);
        state.editingId = data.id;

        document.getElementById("levelName").value = data.levelName;
        document.getElementById("minPoints").value = data.minPoints;
        document.getElementById("maxPoints").value = data.maxPoints;
        document.getElementById("benefits").value = data.benefits || "";

        modalTitle.textContent = `Chỉnh sửa cấp #${data.id}`;
        levelModal.show();
    } catch (err) {
        showToast("Không tải được dữ liệu cấp này!", "danger");
        console.error(err);
    }
}
function onDelete(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name || "(Không rõ)";
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));
    const confirmMsg = document.getElementById('confirmMessage');
    const confirmOkBtn = document.getElementById('confirmOkBtn');

    confirmMsg.textContent = `Bạn có chắc chắn muốn xóa cấp "${name}" không?`;

    const newBtn = confirmOkBtn.cloneNode(true);
    confirmOkBtn.parentNode.replaceChild(newBtn, confirmOkBtn);

    newBtn.addEventListener("click", async () => {
        confirmModal.hide();
        try {
            await membershipLevelApi.remove(id);
            showToast("Đã xóa cấp thành viên thành công!");
            fetchPage();
        } catch (err) {
            console.error(" Lỗi khi xóa:", err);
            showToast("Xóa thất bại!", "danger");
        }
    });

    confirmModal.show();
}


// ===== Form Submit =====
formEl.addEventListener("submit", async (e) => {
    e.preventDefault();
    btnSave.disabled = true;

    const payload = {
        levelName: document.getElementById("levelName").value.trim(),
        minPoints: Number(document.getElementById("minPoints").value),
        maxPoints: Number(document.getElementById("maxPoints").value),
        benefits: document.getElementById("benefits").value.trim(),
    };

    try {
        if (state.editingId) {
            await membershipLevelApi.update(state.editingId, payload);
            showToast("Cập nhật cấp thành viên thành công!");
        } else {
            await membershipLevelApi.create(payload);
            showToast("Thêm cấp thành viên mới thành công!");
        }

        levelModal.hide();
        formEl.reset();
        state.editingId = null;
        fetchPage();
    } catch (err) {
        showToast("Lưu thất bại. Kiểm tra dữ liệu nhập!", "danger");
        console.error(err);
    } finally {
        btnSave.disabled = false;
    }
});

btnCreate.addEventListener("click", () => {
    state.editingId = null;
    formEl.reset();
    modalTitle.textContent = "Thêm cấp độ thành viên mới";
    levelModal.show();
});

function showToast(message, type = "success") {
    const container = document.getElementById("toastContainer");
    if (!container) {
        console.warn("Không tìm thấy #toastContainer!");
        alert(message);
        return;
    }

    const toast = document.createElement("div");
    toast.className = `toast align-items-center text-white border-0 show bg-${type === "success" ? "success" : "danger"}`;
    toast.role = "alert";
    toast.innerHTML = `
      <div class="d-flex">
        <div class="toast-body">${message}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>
    `;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}
fetchPage();
