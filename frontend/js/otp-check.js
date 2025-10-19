import { API_BASE_URL, handleResponse, getValidToken } from "./api.js";

const form = document.getElementById("otp-form");
const otpInput = document.getElementById("otpCode");
const resultCard = document.getElementById("result-card");
const ticketInfo = document.getElementById("ticket-info");

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const code = otpInput.value.trim();

    if (code.length !== 6) {
        Swal.fire({
            icon: "warning",
            title: "OTP không hợp lệ",
            text: "Vui lòng nhập đúng 6 chữ số OTP!",
            confirmButtonColor: "#0aa3ff"
        });
        return;
    }

    // ✅ Lấy token đăng nhập hiện tại
    const token = getValidToken();
    if (!token) {
        Swal.fire({
            icon: "warning",
            title: "Bạn chưa đăng nhập!",
            text: "Chỉ nhân viên hoặc quản lý mới được phép xác thực OTP.",
            confirmButtonColor: "#e50914"
        });
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/otp/check`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}` // ✅ Gửi token kèm request
            },
            body: JSON.stringify({ code })
        });

        const data = await handleResponse(res);

        Swal.fire({
            icon: "success",
            title: "✅ OTP hợp lệ!",
            text: "Dưới đây là thông tin vé của khách hàng.",
            confirmButtonColor: "#0aa3ff"
        });

        showTicket(data);

    } catch (err) {
        Swal.fire({
            icon: "error",
            title: "Xác thực thất bại",
            text: err.message || "Không thể xác thực OTP!",
            confirmButtonColor: "#e50914"
        });
        resultCard.style.display = "none";
    }
});

function showTicket(ticket) {
    resultCard.style.display = "block";
    ticketInfo.innerHTML = `
        <div class="mb-2"><span class="info-label">🎬 Phim:</span> <span class="info-value">${ticket.movieTitle}</span></div>
        <div class="mb-2"><span class="info-label">🏢 Chi nhánh:</span> <span class="info-value">${ticket.branchName}</span></div>
        <div class="mb-2"><span class="info-label">🏟️ Phòng chiếu:</span> <span class="info-value">${ticket.auditoriumName}</span></div>
        <div class="mb-2"><span class="info-label">🗣️ Ngôn ngữ:</span> <span class="info-value">${ticket.language}</span></div>
        <div class="mb-2"><span class="info-label">⏰ Giờ bắt đầu:</span> <span class="info-value">${formatDate(ticket.startTime)}</span></div>
        <div class="mb-2"><span class="info-label">🎟️ Ghế:</span> <span class="info-value">${ticket.seats?.join(", ") || "Không có"}</span></div>
        <div class="mb-2"><span class="info-label">🍿 Combo:</span> <span class="info-value">${ticket.combos?.join(", ") || "Không có"}</span></div>
        <hr/>
        <div class="mb-2"><span class="info-label">💰 Tổng tiền:</span> <span class="info-value">${formatCurrency(ticket.totalPrice)}</span></div>
        <div class="mb-2"><span class="info-label">💳 Thanh toán:</span> <span class="info-value">${ticket.paymentMethod}</span></div>
        <div class="mb-2"><span class="info-label">📄 Trạng thái vé:</span> <span class="info-value">${ticket.ticketStatus}</span></div>
    `;
}

function formatDate(isoStr) {
    if (!isoStr) return "—";
    const d = new Date(isoStr);
    return d.toLocaleString("vi-VN", { hour12: false });
}

function formatCurrency(vnd) {
    if (vnd == null) return "—";
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd);
}
