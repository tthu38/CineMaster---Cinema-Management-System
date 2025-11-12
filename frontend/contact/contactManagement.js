import { API_BASE_URL, handleResponse, getValidToken } from "../js/api/config.js";

/* ======================= GLOBAL VAR ======================= */
let allContacts = [];
let filteredContacts = [];
let currentPage = 0;
const pageSize = 10;

/* ======================= TOAST & CONFIRM ======================= */
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
    const modalEl = document.getElementById("confirmModal");
    const messageEl = document.getElementById("confirmMessage");
    const okBtn = document.getElementById("confirmOkBtn");
    const modal = new bootstrap.Modal(modalEl);

    messageEl.textContent = message;
    modal.show();

    const handleOk = () => {
        modal.hide();
        okBtn.removeEventListener("click", handleOk);
        if (onConfirm) onConfirm();
    };

    okBtn.addEventListener("click", handleOk);
}

/* ======================= MAIN ======================= */
document.addEventListener("DOMContentLoaded", async () => {
    const token = getValidToken();
    const branchId = localStorage.getItem("branchId");
    const role = localStorage.getItem("role");

    if (!token || !["Staff", "Manager", "Admin"].includes(role)) {
        Swal.fire("Lỗi", "Vui lòng đăng nhập hợp lệ.", "error");
        window.location.href = "../user/login.html";
        return;
    }

    if (role === "Admin") {
        await loadBranchOptions(token);
        await loadContactsAll(token);
    } else {
        document.getElementById("branchFilter").style.display = "none";
        await loadContactsByBranch(branchId, token);
    }

    document.getElementById("statusFilter").addEventListener("change", () => applyFilters(0));
    document.getElementById("branchFilter")?.addEventListener("change", () => applyFilters(0));
});

/* ======================= LOAD DATA ======================= */
async function loadBranchOptions(token) {
    const branchSelect = document.getElementById("branchFilter");
    try {
        const res = await fetch(`${API_BASE_URL}/branches`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        const branches = await handleResponse(res);
        branchSelect.innerHTML = `<option value="">Tất cả chi nhánh</option>`;
        branches.forEach(b => {
            const id = b.id || b.branchID;
            const opt = document.createElement("option");
            opt.value = id;
            opt.textContent = b.branchName;
            branchSelect.appendChild(opt);
        });
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
    }
}

async function loadContactsAll(token) {
    try {
        const res = await fetch(`${API_BASE_URL}/contacts/all`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        allContacts = await handleResponse(res);
        console.log("📦 Dữ liệu contacts:", allContacts);
        applyFilters(0);
    } catch (err) {
        console.error("❌ Lỗi tải contacts:", err);
    }
}

async function loadContactsByBranch(branchId, token) {
    try {
        const res = await fetch(`${API_BASE_URL}/contacts/branch/${branchId}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        allContacts = await handleResponse(res);
        console.log("📦 Dữ liệu contacts theo chi nhánh:", allContacts);
        applyFilters(0);
    } catch (err) {
        console.error("❌ Lỗi tải contacts theo chi nhánh:", err);
    }
}

/* ======================= FILTER ======================= */
function applyFilters(page = 0) {
    const role = localStorage.getItem("role");
    const status = document.getElementById("statusFilter").value.trim().toLowerCase();
    const branch = document.getElementById("branchFilter")?.value || "";

    filteredContacts = [...allContacts];

    // ✅ Lọc chi nhánh (Admin)
    if (role === "Admin" && branch) {
        filteredContacts = filteredContacts.filter(c => {
            const bid = c.branch?.id ?? c.branch?.branchID ?? c.branchId ?? c.branchID ?? null;
            const bname = c.branch?.branchName ?? c.branchName ?? "";
            const selectedBranchName =
                document.getElementById("branchFilter").selectedOptions[0]?.textContent?.trim().toLowerCase();

            if (bid && String(bid) === String(branch)) return true;
            if (bname && selectedBranchName && bname.trim().toLowerCase() === selectedBranchName)
                return true;
            return false;
        });
    }

    // ✅ Lọc trạng thái
    if (status) {
        filteredContacts = filteredContacts.filter(c =>
            (c.status || "").trim().toLowerCase() === status
        );
    }

    renderPage(page);
}

/* ======================= TABLE + PAGINATION ======================= */
function renderPage(page) {
    currentPage = page;
    const start = page * pageSize;
    const end = start + pageSize;
    const pageData = filteredContacts.slice(start, end);
    renderTable(pageData);
    renderPagination();
}

function renderTable(contacts) {
    const tbody = document.getElementById("contactTableBody");
    tbody.innerHTML = "";
    const role = localStorage.getItem("role");

    if (!contacts || contacts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-3">Không có liên hệ nào.</td></tr>`;
        return;
    }

    contacts.forEach((c, i) => {
        const canEdit = role === "Staff"; // chỉ Staff được sửa
        const viewBtn = `
            <a href="contactDetail.html?contactId=${c.contactID}" 
               class="btn btn-info btn-sm fw-semibold me-2 rounded-pill px-3 shadow-sm">
               Xem
            </a>`;
        const editBtn = canEdit
            ? `<button class="btn btn-warning btn-sm fw-semibold rounded-pill px-3 shadow-sm"
                        onclick="handleContact(${c.contactID})">
                 Sửa
               </button>`
            : "";

        tbody.innerHTML += `
        <tr>
            <td class="text-center-cell">${i + 1 + currentPage * pageSize}</td>
            <td>${c.fullName}</td>
            <td>${c.email}</td>
            <td>${c.subject}</td>
            <td class="text-center-cell">
                <span class="status-pill ${getStatusColor(c.status)}">
                    ${getStatusLabel(c.status)}
                </span>
            </td>
            <td class="text-center-cell">
                ${c.handledAt ? c.handledAt.replace("T", " ") : "—"}
            </td>
            <td class="text-center-cell">
                <div class="action-btns">
                    <a href="contactDetail.html?contactId=${c.contactID}" 
                       class="btn btn-info btn-sm">
                       <i class="fa-solid fa-eye me-1"></i> Xem
                    </a>
                    ${canEdit
                    ? `<button class="btn btn-warning btn-sm" onclick="handleContact(${c.contactID})">
                               <i class="fa-solid fa-pen me-1"></i> Sửa
                           </button>`
                    : ""}
                </div>
            </td>
        </tr>`;
    });
}

function getStatusColor(status) {
    switch ((status || "").toLowerCase()) {
        case "pending": return "bg-warning text-dark";
        case "processing": return "bg-primary";
        case "resolved": return "bg-success";
        case "rejected": return "bg-danger";
        default: return "bg-secondary";
    }
}
function getStatusLabel(status) {
    switch ((status || "").toLowerCase()) {
        case "pending": return "Chờ xử lý";
        case "processing": return "Đang xử lý";
        case "resolved": return "Đã xử lý";
        case "rejected": return "Từ chối";
        default: return "Không xác định";
    }
}



function renderPagination() {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";
    const totalPages = Math.ceil(filteredContacts.length / pageSize);
    if (totalPages <= 1) return;

    const createBtn = (page, label, disabled = false, active = false) => `
        <button class="btn btn-sm ${active ? "btn-primary" : "btn-secondary"} me-1"
            ${disabled ? "disabled" : ""}
            onclick="goToPage(${page})">${label}</button>`;

    pagination.innerHTML += createBtn(currentPage - 1, "&laquo;", currentPage === 0);
    for (let i = 0; i < totalPages; i++) {
        pagination.innerHTML += createBtn(i, i + 1, false, i === currentPage);
    }
    pagination.innerHTML += createBtn(currentPage + 1, "&raquo;", currentPage === totalPages - 1);
}

window.goToPage = (page) => renderPage(page);

/* ======================= ACTIONS ======================= */
window.handleContact = async (id) => {
    const role = localStorage.getItem("role");
    if (role !== "Staff") {
        showToast(" Chỉ nhân viên mới được cập nhật trạng thái!", "error");
        return;
    }

    const contact = allContacts.find(c => c.contactID === id);
    const currentStatus = contact?.status || "Pending";
    const currentNote = contact?.handleNote || "";

    const { value: formValues } = await Swal.fire({
        title: `<h5 class="fw-bold text-info mb-3">
                   <i class="fa-solid fa-pen-to-square me-2"></i>Cập nhật trạng thái liên hệ #${id}
                </h5>`,
        html: `
            <div class="text-start">
                <label class="form-label text-light fw-semibold">Trạng thái</label>
                <select id="statusSelect" class="form-select mb-3" style="
                    background: rgba(12, 24, 45, 0.6);
                    color: #e0e6f5;
                    border: 1px solid rgba(34,193,255,0.3);
                    border-radius: 10px;
                ">
                    <option value="Processing" ${currentStatus === "Processing" ? "selected" : ""}>Đang xử lý</option>
                    <option value="Resolved" ${currentStatus === "Resolved" ? "selected" : ""}>Đã xử lý</option>
                    <option value="Rejected" ${currentStatus === "Rejected" ? "selected" : ""}>Từ chối</option>
                </select>

                <label class="form-label text-light fw-semibold">Ghi chú xử lý</label>
                <textarea id="noteInput" class="form-control" rows="3" placeholder="Nhập ghi chú (tuỳ chọn)" 
                    style="
                        background: rgba(12, 24, 45, 0.6);
                        color: #e0e6f5;
                        border: 1px solid rgba(34,193,255,0.3);
                        border-radius: 10px;
                    ">${currentNote}</textarea>
            </div>
        `,
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: "💾 Lưu thay đổi",
        cancelButtonText: "Hủy",
        confirmButtonColor: "#00bfff",
        background: "rgba(11, 22, 45, 0.95)",
        color: "#e0e6f5",
        customClass: {
            popup: "shadow-lg rounded-4 border border-info",
            confirmButton: "rounded-pill px-4 fw-semibold text-dark",
            cancelButton: "rounded-pill px-4 fw-semibold",
        },
        preConfirm: () => ({
            status: document.getElementById("statusSelect").value,
            handleNote: document.getElementById("noteInput").value.trim(),
        }),
    });

    if (!formValues) return;

    try {
        const token = localStorage.getItem("accessToken");
        const res = await fetch(`${API_BASE_URL}/contacts/${id}/update`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(formValues),
        });
        await handleResponse(res);
        showToast(" Cập nhật trạng thái thành công!");
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        showToast(" Lỗi khi cập nhật trạng thái!", "error");
        console.error(err);
    }
};
