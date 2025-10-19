import { API_BASE_URL, getValidToken, handleResponse } from "../js/api.js";

const ticketBody = document.getElementById("ticketBody");
const statusFilter = document.getElementById("statusFilter");
const searchBox = document.getElementById("searchBox");
const refreshBtn = document.getElementById("refreshBtn");

let allTickets = [];

async function loadTickets() {
    try {
        const token = getValidToken();
        const branchId = localStorage.getItem("branchId");

        const res = await fetch(`${API_BASE_URL}/tickets/branch/${branchId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const json = await handleResponse(res);
        allTickets = json.data || json.result || json;
        renderTickets(allTickets);
    } catch (err) {
        console.error("❌ Lỗi tải vé:", err);
        ticketBody.innerHTML = `<tr><td colspan="9" class="text-danger">Không thể tải danh sách vé.</td></tr>`;
    }
}

function renderTickets(data) {
    if (!data || data.length === 0) {
        ticketBody.innerHTML = `<tr><td colspan="9" class="text-muted">Không có vé nào.</td></tr>`;
        return;
    }

    ticketBody.innerHTML = data.map((t, i) => `
        <tr>
            <td>${i + 1}</td>
            <td>${t.movieTitle}</td>
            <td>${formatDate(t.showtimeStart)}</td>
            <td>${t.branchName}</td>
            <td>${t.seatNumbers || "-"}</td>
            <td>${t.customerName || "Khách vãng lai"}</td>
            <td>${(t.totalPrice || 0).toLocaleString()} đ</td>
            <td><span class="badge ${t.ticketStatus}">${translateStatus(t.ticketStatus)}</span></td>
            <td>${renderActionButtons(t)}</td>
        </tr>
    `).join("");
}

/* ✅ Chỉ cho phép hành động khi phù hợp trạng thái:
   - CancelRequested → hiển thị “Duyệt hủy vé”
   - Cancelled → hiển thị “Hoàn tiền”
   - Các trạng thái khác (Booked, Used, Refunded) → chỉ nút “Xem”
*/
function renderActionButtons(t) {
    const id = t.ticketId;
    switch (t.ticketStatus) {
        case "CancelRequested":
            return `
                <button class="btn btn-sm btn-outline-warning me-1" onclick="approveCancel(${id})">
                    <i class="fa-solid fa-check"></i> Duyệt hủy
                </button>`;
        case "Cancelled":
            return `
                <button class="btn btn-sm btn-outline-info me-1" onclick="approveRefund(${id})">
                    <i class="fa-solid fa-money-bill-transfer"></i> Hoàn tiền
                </button>`;
        default:
            return `
                <button class="btn btn-sm btn-outline-light" onclick="viewTicket(${id})">
                    <i class="fa-regular fa-eye"></i> Xem
                </button>`;
    }
}

// ====== Các thao tác ======
window.viewTicket = function(id) {
    window.open(`../tickets/ticketDetail.html?ticketId=${id}`, "_blank");
};

// ✅ Duyệt hủy vé
window.approveCancel = async function(ticketId) {
    const token = getValidToken();
    const staffId = localStorage.getItem("accountId");
    const ok = await Swal.fire({
        title: "Xác nhận duyệt hủy vé?",
        text: "Vé sẽ chuyển sang trạng thái 'Đã hủy'.",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Duyệt hủy"
    });
    if (!ok.isConfirmed) return;

    try {
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/approve-cancel?accountId=${staffId}`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` }
        });
        await handleResponse(res);
        Swal.fire("✅ Đã duyệt", "Vé đã được hủy.", "success");
        loadTickets();
    } catch (err) {
        Swal.fire("❌ Lỗi", "Không thể duyệt hủy vé.", "error");
    }
};

// 💰 Duyệt hoàn tiền
window.approveRefund = async function(ticketId) {
    const token = getValidToken();
    const staffId = localStorage.getItem("accountId");
    const confirm = await Swal.fire({
        title: "Xác nhận hoàn tiền?",
        text: "Sau khi duyệt, vé sẽ chuyển sang trạng thái 'Hoàn tiền'.",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Hoàn tiền"
    });
    if (!confirm.isConfirmed) return;

    try {
        const res = await fetch(`${API_BASE_URL}/tickets/${ticketId}/approve-refund?accountId=${staffId}`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${token}` }
        });
        await handleResponse(res);
        Swal.fire("✅ Thành công", "Đã hoàn tiền cho vé.", "success");
        loadTickets();
    } catch (err) {
        Swal.fire("❌ Lỗi", "Không thể hoàn tiền vé.", "error");
        console.error(err);
    }
};

// ====== Lọc / Tìm kiếm ======
statusFilter.addEventListener("change", applyFilters);
searchBox.addEventListener("input", applyFilters);
refreshBtn.addEventListener("click", loadTickets);

function applyFilters() {
    let filtered = allTickets;
    const st = statusFilter.value;
    const kw = searchBox.value.trim().toLowerCase();

    if (st) filtered = filtered.filter(t => t.ticketStatus === st);
    if (kw) filtered = filtered.filter(t =>
        t.movieTitle.toLowerCase().includes(kw) ||
        t.branchName.toLowerCase().includes(kw) ||
        (t.customerName?.toLowerCase().includes(kw))
    );
    renderTickets(filtered);
}

// ====== Helpers ======
function formatDate(d) {
    return new Date(d).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" });
}

function translateStatus(st) {
    switch (st) {
        case "Booked": return "Đã đặt";
        case "Used": return "Đã sử dụng";
        case "Cancelled": return "Đã hủy";
        case "Refunded": return "Hoàn tiền";
        case "CancelRequested": return "Chờ hủy";
        default: return st;
    }
}

loadTickets();
