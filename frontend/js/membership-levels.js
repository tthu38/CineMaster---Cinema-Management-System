import { membershipLevelApi } from './api.js';

// ===== DOM =====
const tblBody = document.getElementById("tblBody");
const formEl = document.getElementById("levelForm");
const levelModal = new bootstrap.Modal(document.getElementById("levelModal"));
const btnSave = document.getElementById("btnSave");
const btnCreate = document.getElementById("btnCreate");

// ===== State =====
let state = {
    page: 0,
    size: 10,
    sort: "id,asc",
    keyword: "",
    editingId: null
};

// ===== Render =====
function renderTable(page) {
    const data = page.content || [];
    if (!data.length) {
        tblBody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">Không có dữ liệu</td></tr>`;
        return;
    }

    tblBody.innerHTML = data.map(x => `
        <tr data-id="${x.id}">
            <td>${x.id}</td>
            <td>${x.levelName}</td>
            <td><span class="badge-soft">${x.minPoints} – ${x.maxPoints}</span></td>
            <td>${x.benefits || ''}</td>
            <td class="text-end">
                <button class="btn btn-outline btn-sm me-1 act-edit"><i class="fa-solid fa-pen"></i></button>
                <button class="btn btn-outline btn-sm text-danger act-del"><i class="fa-solid fa-trash"></i></button>
            </td>
        </tr>
    `).join('');

    // Gán sự kiện edit/xóa
    document.querySelectorAll(".act-edit").forEach(btn => btn.addEventListener("click", onEdit));
    document.querySelectorAll(".act-del").forEach(btn => btn.addEventListener("click", onDelete));
}

// ===== Fetch Page =====
async function fetchPage() {
    try {
        const page = await membershipLevelApi.search({
            page: state.page,
            size: state.size,
            sort: state.sort,
            keyword: state.keyword
        });
        renderTable(page);
    } catch (err) {
        console.error(err);
        tblBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">Không tải được dữ liệu</td></tr>`;
    }
}

// ===== Edit =====
async function onEdit(e) {
    const id = e.currentTarget.closest("tr").dataset.id;
    const data = await membershipLevelApi.get(id);
    state.editingId = data.id;

    document.getElementById("levelName").value = data.levelName;
    document.getElementById("minPoints").value = data.minPoints;
    document.getElementById("maxPoints").value = data.maxPoints;
    document.getElementById("benefits").value = data.benefits || '';

    levelModal.show();
}

// ===== Delete =====
async function onDelete(e) {
    const id = e.currentTarget.closest("tr").dataset.id;
    if (!confirm(`Xóa cấp #${id}?`)) return;
    await membershipLevelApi.remove(id);
    fetchPage();
}

// ===== Form Submit =====
formEl.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        levelName: document.getElementById("levelName").value.trim(),
        minPoints: Number(document.getElementById("minPoints").value),
        maxPoints: Number(document.getElementById("maxPoints").value),
        benefits: document.getElementById("benefits").value.trim()
    };

    if (state.editingId) {
        await membershipLevelApi.update(state.editingId, payload);
    } else {
        await membershipLevelApi.create(payload);
    }

    levelModal.hide();
    state.editingId = null;
    fetchPage();
});

// ===== Create Button =====
btnCreate.addEventListener("click", () => {
    state.editingId = null;
    formEl.reset();
    levelModal.show();
});

// ===== Init =====
fetchPage();
