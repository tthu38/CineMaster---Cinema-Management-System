import { API_BASE_URL, handleResponse, getValidToken } from "../js/api.js";

const accountId = localStorage.getItem("accountId");
const ticketList = document.getElementById("ticketList");

if (!accountId) {
    ticketList.innerHTML = `<p style="text-align:center;color:#f66;">Bạn cần đăng nhập để xem vé.</p>`;
} else {
    loadTickets();
}

/* ============================================================
   🔹 1️⃣ Hàm tải danh sách vé theo người dùng
   ============================================================ */
async function loadTickets() {
    try {
        const token = getValidToken();
        const res = await fetch(`${API_BASE_URL}/tickets/account/${accountId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const tickets = await handleResponse(res);

        renderTickets(tickets);
        setupFilter(tickets);
    } catch (err) {
        console.error("❌ Lỗi tải vé:", err);
        ticketList.innerHTML = `<p style="text-align:center;color:#f66;">Không thể tải danh sách vé.</p>`;
    }
}

/* ============================================================
   🔹 2️⃣ Hàm render danh sách vé
   ============================================================ */
function renderTickets(data) {
    if (!data || data.length === 0) {
        ticketList.innerHTML = `
          <div class="empty-state">
              <i class="fa-solid fa-ticket"></i>
              <h4>Không có vé nào</h4>
              <p>Bạn chưa đặt vé nào. Hãy chọn phim yêu thích để đặt vé nhé!</p>
          </div>`;
        return;
    }

    ticketList.innerHTML = data.map(t => `
    <div class="ticket-card" data-id="${t.ticketId}" data-status="${t.ticketStatus}">
        <div class="ticket-info" style="cursor:pointer;">
            <h5 class="movie-title">${t.movieTitle || "Không xác định"}</h5>
            <p><i class="fa-regular fa-clock"></i> ${formatDate(t.showtimeStart)}</p>
            <p><i class="fa-solid fa-location-dot"></i> ${t.branchName || "Không rõ rạp"}</p>
            <p><i class="fa-solid fa-chair"></i> Ghế: ${t.seatNumbers || "N/A"}</p>
            <p><i class="fa-solid fa-money-bill"></i> ${t.totalPrice?.toLocaleString()} đ</p>
        </div>
        <div class="d-flex flex-column align-items-center gap-2">
            <span class="ticket-status ${t.ticketStatus}">
                ${translateStatus(t.ticketStatus)}
            </span>
            ${t.ticketStatus === "Booked" ? `
                <button class="btn btn-sm btn-outline-danger cancel-btn" data-id="${t.ticketId}">
                    <i class="fa-solid fa-xmark"></i> Hủy vé
                </button>
            ` : ""}
        </div>
    </div>
`).join("");

    document.querySelectorAll(".cancel-btn").forEach(btn => {
        btn.addEventListener("click", async e => {
            e.stopPropagation(); // tránh mở chi tiết vé
            const id = btn.dataset.id;
            const token = getValidToken();
            const confirm = await Swal.fire({
                title: "Xác nhận hủy vé?",
                text: "Bạn có chắc muốn gửi yêu cầu hủy vé này?",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Gửi yêu cầu",
                cancelButtonText: "Không"
            });
            if (!confirm.isConfirmed) return;

            try {
                const res = await fetch(`${API_BASE_URL}/tickets/${id}/cancel-request`, {
                    method: "PUT",
                    headers: { "Authorization": `Bearer ${token}` }
                });
                await handleResponse(res);
                Swal.fire("✅ Thành công", "Đã gửi yêu cầu hủy vé.", "success");
                loadTickets();
            } catch (err) {
                Swal.fire("❌ Lỗi", "Không thể hủy vé.", "error");
                console.error(err);
            }
        });
    });


    // ✅ Gắn sự kiện click để mở ticketDetail.html
    document.querySelectorAll(".ticket-card").forEach(card => {
        card.addEventListener("click", () => {
            const id = card.dataset.id;
            window.location.href = `ticketDetail.html?ticketId=${id}`;
        });
    });
}

/* ============================================================
   🔹 3️⃣ Bộ lọc vé theo trạng thái
   ============================================================ */
function setupFilter(tickets) {
    const buttons = document.querySelectorAll(".filter-btn");
    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const status = btn.dataset.status;
            const filtered = status === "all" ? tickets : tickets.filter(t => t.ticketStatus === status);
            renderTickets(filtered);
        });
    });
}

/* ============================================================
   🔹 4️⃣ Các hàm tiện ích
   ============================================================ */
function translateStatus(st) {
    switch (st) {
        case "Booked": return "Đã đặt";
        case "Used": return "Đã sử dụng";
        case "Cancelled": return "Đã hủy";
        case "Refunded": return "Đã hoàn tiền";
        case "CancelRequested": return "Chờ hủy";
        default: return st;
    }
}

function formatDate(dt) {
    if (!dt) return "";
    return new Date(dt).toLocaleString("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}
