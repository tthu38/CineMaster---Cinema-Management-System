import { API_BASE_URL, getValidToken, handleResponse } from "../js/api.js";

const ticketBody = document.getElementById("ticketBody");
const statusFilter = document.getElementById("statusFilter");
const searchBox = document.getElementById("searchBox");
const refreshBtn = document.getElementById("refreshBtn");

let allTickets = [];

/* ============================================================
   🔹 1️⃣ Tải danh sách vé + tự động bổ sung thông tin còn thiếu
   ============================================================ */
async function loadTickets() {
    try {
        const token = getValidToken();
        const branchId = localStorage.getItem("branchId");

        const res = await fetch(`${API_BASE_URL}/tickets/branch/${branchId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        let data = await handleResponse(res);
        data = data.data || data.result || data;

        // 🟦 Nếu vé thiếu thông tin, tự động gọi thêm API chi tiết
        const enriched = await Promise.all(data.map(async (t) => {
            if (!t.movieTitle || !t.showtimeStart || !t.seatNumbers) {
                try {
                    const resDetail = await fetch(`${API_BASE_URL}/tickets/${t.ticketId}`, {
                        headers: { "Authorization": `Bearer ${token}` }
                    });
                    const detail = await handleResponse(resDetail);
                    return { ...t, ...detail };
                } catch (err) {
                    console.warn("⚠️ Không thể lấy chi tiết vé:", t.ticketId, err);
                    return t;
                }
            }
            return t;
        }));

        allTickets = enriched;
        renderTickets(allTickets);
    } catch (err) {
        console.error("❌ Lỗi tải vé:", err);
        ticketBody.innerHTML = `<tr><td colspan="9" class="text-danger">Không thể tải danh sách vé.</td></tr>`;
    }
}

/* ============================================================
   🔹 2️⃣ Hiển thị danh sách vé trong bảng
   ============================================================ */

function renderTickets(data) {
    if (!data || data.length === 0) {
        ticketBody.innerHTML = `<tr><td colspan="9" class="text-muted">Không có vé nào.</td></tr>`;
        return;
    }

    ticketBody.innerHTML = data.map((t, i) => {
        // 🎬 Phim
        const movieTitle = t.movieTitle || "Không rõ";

        // 🕒 Suất chiếu
        const showtime = t.startTime || t.showtimeStart || "-";

        // 🏢 Rạp
        const branchName = t.branchName || "-";

        // 💺 Ghế
        const seatNums = t.seatNames || "-";

        // 👤 Khách hàng
        // 👤 Khách hàng
        const customer = t.account?.fullName || t.account?.username || t.customerName || t.customer?.fullName || "Khách vãng lai";

        return `
        <tr>
            <td>${i + 1}</td>
            <td>${movieTitle}</td>
            <td>${formatDate(showtime)}</td>
            <td>${branchName}</td>
            <td>${seatNums}</td>
            <td>${customer}</td>
            <td>${(t.totalPrice || 0).toLocaleString()} đ</td>
            <td><span class="badge ${t.ticketStatus}">${translateStatus(t.ticketStatus)}</span></td>
            <td>${renderActionButtons(t)}</td>
        </tr>`;
    }).join("");
}


/* ============================================================
   🔹 3️⃣ Nút hành động
   ============================================================ */
function renderActionButtons(t) {
    const id = t.ticketId;
    switch (t.ticketStatus) {
        case "CANCEL_REQUESTED":
            return `
                <button class="btn btn-sm btn-outline-warning me-1" onclick="approveCancel(${id})">
                    <i class="fa-solid fa-check"></i> Duyệt hủy
                </button>`;
        case "CANCELLED":
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

/* ============================================================
   🔹 4️⃣ Xem chi tiết vé
   ============================================================ */
window.viewTicket = async function (id) {
    const token = getValidToken();

    try {
        // Dữ liệu sơ bộ trong bảng
        let ticket = allTickets.find(t => t.ticketId == id) || {};

        // Nếu dữ liệu thiếu thì fetch thêm chi tiết
        const needMore = !ticket.movieTitle || !ticket.showtimeStart || !ticket.seatNumbers;
        if (needMore) {
            const res = await fetch(`${API_BASE_URL}/tickets/${id}`, {
                headers: { "Authorization": `Bearer ${token}` }
            });
            const detail = await handleResponse(res);
            ticket = { ...ticket, ...detail };
        }

        const detailData = {
            ticketId: ticket.ticketId,
            movieTitle: ticket.movieTitle,
            movieGenre: ticket.movieGenre || "Không rõ",
            movieDuration: ticket.movieDuration || "?",
            branchName: ticket.branchName,
            auditoriumName: ticket.auditoriumName,
            showtimeStart: ticket.startTime || ticket.showtimeStart,
            showtimeEnd: ticket.showtimeEnd,
            // ✅ fix 2 dòng này:
            seatNumbers: ticket.seatNames || ticket.seatNumbers || "N/A",
            comboList: ticket.combos?.map(c => c.comboName).join(", ") || "Không có",

            totalPrice: ticket.totalPrice,
            paymentMethod: ticket.paymentMethod || "CASH",
            ticketStatus: ticket.ticketStatus
        };

        localStorage.setItem("ticketDetailData", JSON.stringify(detailData));
        window.open(`../tickets/ticketDetail.html?ticketId=${id}`, "_blank");
    } catch (err) {
        console.error("❌ Lỗi mở chi tiết vé:", err);
        Swal.fire("Lỗi", "Không thể mở chi tiết vé.", "error");
    }
};

/* ============================================================
   🔹 5️⃣ Duyệt hủy vé
   ============================================================ */
window.approveCancel = async function (ticketId) {
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
            headers: { "Authorization": `Bearer ${token}` }
        });
        await handleResponse(res);
        Swal.fire("✅ Đã duyệt", "Vé đã được hủy.", "success");
        loadTickets();
    } catch (err) {
        Swal.fire("❌ Lỗi", "Không thể duyệt hủy vé.", "error");
    }
};

/* ============================================================
   🔹 6️⃣ Duyệt hoàn tiền
   ============================================================ */
window.approveRefund = async function (ticketId) {
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
            headers: { "Authorization": `Bearer ${token}` }
        });
        await handleResponse(res);
        Swal.fire("✅ Thành công", "Đã hoàn tiền cho vé.", "success");
        loadTickets();
    } catch (err) {
        Swal.fire("❌ Lỗi", "Không thể hoàn tiền vé.", "error");
        console.error(err);
    }
};

/* ============================================================
   🔹 7️⃣ Lọc + tìm kiếm vé
   ============================================================ */
statusFilter.addEventListener("change", applyFilters);
searchBox.addEventListener("input", applyFilters);
refreshBtn.addEventListener("click", loadTickets);

function applyFilters() {
    let filtered = allTickets;
    const st = statusFilter.value;
    const kw = searchBox.value.trim().toLowerCase();

    if (st) filtered = filtered.filter(t => t.ticketStatus === st);
    if (kw) filtered = filtered.filter(t =>
        t.movieTitle?.toLowerCase().includes(kw) ||
        t.branchName?.toLowerCase().includes(kw) ||
        t.customerName?.toLowerCase().includes(kw)
    );

    renderTickets(filtered);
}

/* ============================================================
   🔹 8️⃣ Hàm tiện ích
   ============================================================ */
function formatDate(d) {
    if (!d) return "-";
    const date = new Date(d);
    if (isNaN(date)) return "-";
    return date.toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" });
}

function translateStatus(st) {
    switch (st) {
        case "BOOKED": return "Đã đặt";
        case "USED": return "Đã sử dụng";
        case "CANCELLED": return "Đã hủy";
        case "REFUNDED": return "Hoàn tiền";
        case "CANCEL_REQUESTED": return "Chờ hủy";
        default: return st;
    }
}

loadTickets();
