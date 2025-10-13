// ================= MEMBERSHIP LEVEL MANAGER =================
import { membershipLevelApi } from './api/membershipLevelApi.js';

// ===== DOM =====
const tblBody = document.getElementById("tblBody");
const formEl = document.getElementById("levelForm");
const levelModal = new bootstrap.Modal(document.getElementById("levelModal"));
const btnSave = document.getElementById("btnSave");
const btnCreate = document.getElementById("btnCreate");
const modalTitle = document.getElementById("levelModalTitle");

const btnPrev = document.getElementById("prevPage");
const btnNext = document.getElementById("nextPage");
const pagingInfo = document.getElementById("pagingInfo");

// ===== State =====
let state = {
    page: 0,
    size: 10,
    sort: "id,ASC",
    totalPages: 1,
    editingId: null,
};

// ===== Render Table =====
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
          <button class="btn btn-outline btn-sm act-edit me-1" title="Sửa"><i class="fa-solid fa-pen"></i></button>
          <button class="btn btn-outline btn-sm text-danger act-del" title="Xóa"><i class="fa-solid fa-trash"></i></button>
        </td>
      </tr>
    `).join('');

    document.querySelectorAll(".act-edit").forEach(btn => btn.addEventListener("click", onEdit));
    document.querySelectorAll(".act-del").forEach(btn => btn.addEventListener("click", onDelete));
}

// ===== Fetch Page =====
async function fetchPage() {
    tblBody.innerHTML = `
      <tr><td colspan="5" class="text-center text-info py-4">Đang tải dữ liệu...</td></tr>`;
    try {
        const page = await membershipLevelApi.list(state.page, state.size, state.sort);
        state.totalPages = page.totalPages;
        renderTable(page);
        pagingInfo.textContent = `Trang ${state.page + 1} / ${state.totalPages}`;
        btnPrev.disabled = state.page === 0;
        btnNext.disabled = state.page >= state.totalPages - 1;
    } catch (err) {
        console.error(err);
        tblBody.innerHTML = `
          <tr><td colspan="5" class="text-center text-danger py-4">Không tải được dữ liệu</td></tr>`;
    }
}

// ===== Pagination =====
btnPrev.addEventListener("click", () => {
    if (state.page > 0) {
        state.page--;
        fetchPage();
    }
});
btnNext.addEventListener("click", () => {
    if (state.page < state.totalPages - 1) {
        state.page++;
        fetchPage();
    }
});

// ===== Edit =====
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

// ===== Delete =====
function onDelete(e) {
    const row = e.currentTarget.closest("tr");
    const id = row.dataset.id;
    const name = row.querySelector("td:nth-child(2)")?.textContent || "(không rõ)";
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));
    const confirmMsg = document.getElementById('confirmMessage');
    const confirmOkBtn = document.getElementById('confirmOkBtn');

    confirmMsg.textContent = `Bạn có chắc chắn muốn xóa cấp "${name}" không?`;

    const newBtn = confirmOkBtn.cloneNode(true);
    confirmOkBtn.parentNode.replaceChild(newBtn, confirmOkBtn);

    newBtn.addEventListener("click", async () => {
        document.activeElement.blur();
        confirmModal.hide();
        try {
            await membershipLevelApi.remove(id);
            showToast("Đã xóa cấp thành viên thành công!");
            fetchPage();
        } catch (err) {
            console.error(err);
            showToast("Lỗi khi xóa cấp thành viên!", "danger");
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

// ===== Create Button =====
btnCreate.addEventListener("click", () => {
    state.editingId = null;
    formEl.reset();
    modalTitle.textContent = "Thêm cấp độ thành viên mới";
    levelModal.show();
});

// ===== Toast =====
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

// ===== Init =====
fetchPage();
