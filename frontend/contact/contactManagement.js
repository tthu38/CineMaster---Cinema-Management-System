import { API_BASE_URL, handleResponse, getValidToken } from "../js/api/config.js";

let allContacts = [];

document.addEventListener("DOMContentLoaded", async () => {
    const token = getValidToken();
    const branchId = localStorage.getItem("branchId");
    const role = localStorage.getItem("role");

    if (!token || !["Staff", "Manager", "Admin"].includes(role)) {
        Swal.fire("Lỗi", "Vui lòng đăng nhập hợp lệ.", "error");
        window.location.href = "../user/login.html";
        return;
    }

    // Admin thấy dropdown chọn chi nhánh
    if (role === "Admin") {
        await loadBranchOptions(token);
        await loadContactsAll(token);
    } else {
        document.getElementById("branchFilter").style.display = "none";
        await loadContactsByBranch(branchId, token);
    }

    // 🎯 Bắt sự kiện lọc trạng thái
    document.getElementById("statusFilter").addEventListener("change", filterContacts);
});

// 🟦 Lọc theo trạng thái
function filterContacts() {
    const selected = document.getElementById("statusFilter").value;
    const filtered = selected
        ? allContacts.filter(c => c.status === selected)
        : allContacts;
    renderTable(filtered);
}

// 🟢 Tải tất cả (Admin)
async function loadContactsAll(token) {
    const res = await fetch(`${API_BASE_URL}/contacts/all`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    allContacts = await handleResponse(res);
    renderTable(allContacts);
}

// 🟢 Tải theo chi nhánh (Manager/Staff)
async function loadContactsByBranch(branchId, token) {
    const res = await fetch(`${API_BASE_URL}/contacts/branch/${branchId}`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    allContacts = await handleResponse(res);
    renderTable(allContacts);
}

// 🟢 Render bảng
function renderTable(contacts) {
    const tbody = document.getElementById("contactTableBody");
    tbody.innerHTML = "";

    if (!contacts || contacts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Không có liên hệ nào.</td></tr>`;
        return;
    }

    contacts.forEach((c, i) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${i + 1}</td>
      <td>${c.fullName}</td>
      <td>${c.email}</td>
      <td>${c.subject}</td>
      <td><span class="status-pill status-${c.status}">${c.status}</span></td>
      <td>${c.handledAt ? c.handledAt.replace("T", " ") : "—"}</td>
      <td>
        <button class="action-btn me-2" title="Xem chi tiết" onclick="viewDetail(${c.contactID})">
          <i class="fa-solid fa-eye"></i>
        </button>
        <button class="action-btn" title="Cập nhật trạng thái" onclick="handleContact(${c.contactID})">
          <i class="fa-solid fa-pen"></i>
        </button>
      </td>
    `;
        tbody.appendChild(tr);
    });
}

// 👁️ Mở trang chi tiết
window.viewDetail = function (contactId) {
    window.location.href = `contactDetail.html?contactId=${contactId}`;
};

// ✏️ Cập nhật trạng thái (Staff ONLY)
window.handleContact = async function (contactId) {
    const role = localStorage.getItem("role");
    if (role !== "Staff") {
        Swal.fire("Thông báo", "Chỉ nhân viên mới được cập nhật trạng thái.", "info");
        return;
    }

    const { value: formValues } = await Swal.fire({
        title: `✏️ Cập nhật trạng thái #${contactId}`,
        html: `
      <select id="statusSelect" class="swal2-select">
        <option value="Processing">Đang xử lý</option>
        <option value="Resolved">Đã xử lý</option>
        <option value="Rejected">Từ chối</option>
      </select>
      <textarea id="noteInput" class="swal2-textarea" placeholder="Ghi chú xử lý (tùy chọn)"></textarea>
    `,
        focusConfirm: false,
        preConfirm: () => ({
            status: document.getElementById("statusSelect").value,
            handleNote: document.getElementById("noteInput").value,
        }),
        showCancelButton: true,
        confirmButtonText: "Cập nhật",
        cancelButtonText: "Hủy",
        confirmButtonColor: "#00bfff",
    });

    if (!formValues) return;

    try {
        const token = localStorage.getItem("accessToken");
        const res = await fetch(`${API_BASE_URL}/contacts/${contactId}/update`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(formValues),
        });
        await handleResponse(res);
        Swal.fire("✅ Thành công", "Đã cập nhật trạng thái!", "success");
        setTimeout(() => location.reload(), 1000);
    } catch (err) {
        Swal.fire("❌ Lỗi", err.message || "Không thể cập nhật.", "error");
    }
};

// 🏢 Load danh sách chi nhánh (Admin)
async function loadBranchOptions(token) {
    const branchSelect = document.getElementById("branchFilter");
    try {
        const res = await fetch(`${API_BASE_URL}/branches`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        const branches = await handleResponse(res);
        branches.forEach((b) => {
            const opt = document.createElement("option");
            opt.value = b.id || b.branchID;
            opt.textContent = b.branchName;
            branchSelect.appendChild(opt);
        });

        branchSelect.addEventListener("change", async (e) => {
            const id = e.target.value;
            if (id) await loadContactsByBranch(id, token);
            else await loadContactsAll(token);
        });
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
    }
}
