import { API_BASE_URL, handleResponse, getValidToken } from "./api.js";
import { seatApi } from "./api/seatApi.js";
import { showtimeApi } from "./api/showtimeApi.js"; // 👈 Dùng fallback khi OTP thiếu auditoriumId




/* ===================== CÁC PHẦN TỬ DOM ===================== */
const form = document.getElementById("otp-form");
const otpInput = document.getElementById("otpCode");
const resultCard = document.getElementById("result-card");
const ticketInfo = document.getElementById("ticket-info");
const scanBtn = document.getElementById("scanQrBtn");
const qrReaderDiv = document.getElementById("qr-reader");


/* ===================== QUÉT QR ===================== */
let qrScanner = null;
let qrIsRunning = false;
let lastDecodedAt = 0; // chống đọc trùng


function extractOtpFromText(text) {
    if (!text) return null;
    const plain = text.trim();
    if (/^\d{6}$/.test(plain)) return plain;
    try {
        const url = new URL(text);
        const qp = url.searchParams.get("otp") || url.searchParams.get("code");
        if (/^\d{6}$/.test(qp || "")) return qp;
    } catch {}
    const m1 = text.match(/(?:otp|code)\s*[:=]\s*(\d{6})/i);
    if (m1) return m1[1];
    try {
        const obj = JSON.parse(text);
        const candidate = obj?.otp ?? obj?.code ?? obj?.data?.otp ?? obj?.data?.code;
        if (/^\d{6}$/.test(String(candidate || ""))) return String(candidate);
    } catch {}
    return null;
}


async function startQr() {
    if (qrIsRunning) return;
    if (!window.Html5Qrcode) {
        Swal.fire({
            icon: "error",
            title: "Thiếu thư viện quét QR",
            text: "Không tìm thấy 'html5-qrcode'. Vui lòng tải lại trang.",
            confirmButtonColor: "#e50914"
        });
        return;
    }


    qrReaderDiv.style.display = "block";
    qrScanner = new Html5Qrcode("qr-reader");
    qrIsRunning = true;


    try {
        await qrScanner.start(
            { facingMode: "environment" },
            { fps: 10, qrbox: 250 },
            async (decodedText) => {
                const now = Date.now();
                if (now - lastDecodedAt < 1200) return;
                lastDecodedAt = now;
                const otp = extractOtpFromText(decodedText);
                if (!otp) return;
                otpInput.value = otp;
                await stopQr();
                form.requestSubmit();
            },
            (errMsg) => { /* bỏ qua lỗi đọc liên tục */ }
        );
    } catch (e) {
        qrIsRunning = false;
        qrReaderDiv.style.display = "none";
        Swal.fire({
            icon: "error",
            title: "Không thể mở camera",
            text: "Kiểm tra quyền camera của trình duyệt hoặc thiết bị.",
            confirmButtonColor: "#e50914"
        });
    }
}


async function stopQr() {
    if (qrScanner && qrIsRunning) {
        try { await qrScanner.stop(); } catch {}
        try { await qrScanner.clear(); } catch {}
    }
    qrIsRunning = false;
    qrReaderDiv.style.display = "none";
}


scanBtn?.addEventListener("click", () => {
    if (qrIsRunning) stopQr();
    else startQr();
});
window.addEventListener("beforeunload", () => stopQr());


/* ===================== GỬI OTP ===================== */
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
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ code })
        });


        const data = await handleResponse(res);
        await stopQr();


        Swal.fire({
            icon: "success",
            title: "✅ Vé hợp lệ!",
            text: "Dưới đây là thông tin vé của khách hàng.",
            confirmButtonColor: "#0aa3ff"
        });


        await showTicket(data); // 👈 đảm bảo dùng hàm chuẩn


    } catch (err) {
        await stopQr();
        Swal.fire({
            icon: "error",
            title: "Xác thực thất bại",
            text: err.message || "Không thể xác thực OTP!",
            confirmButtonColor: "#e50914"
        });
        resultCard.style.display = "none";
    }
});


/* ===================== HIỂN THỊ VÉ ===================== */
async function showTicket(ticket) {
    resultCard.style.display = "block";
    console.log("🎟️ Dữ liệu vé OTP:", ticket);


    // 🧩 Lấy auditoriumId — fallback bằng tên phòng
    let auditoriumId = ticket.auditoriumId;
    let branchName = ticket.branchName || "";


    // Nếu không có ID nhưng có tên phòng -> thử tìm qua API tất cả phòng chi nhánh
    if (!auditoriumId && ticket.auditoriumName && branchName) {
        try {
            // lấy toàn bộ phòng từ API ghế để dò
            const branches = await fetch(`${API_BASE_URL}/branches`);
            const allBranches = await branches.json();
            const branch = allBranches.find(
                b => b.name?.trim() === branchName?.trim() || b.branchName?.trim() === branchName?.trim()
            );


            if (branch) {
                const resAudis = await fetch(`${API_BASE_URL}/auditoriums/branch/${branch.branchId}`);
                const allRooms = await resAudis.json();
                const room = allRooms.find(
                    r => r.name?.trim() === ticket.auditoriumName?.trim()
                );
                auditoriumId = room?.auditoriumId || room?.id;
            }
        } catch (err) {
            console.warn("⚠️ Không thể tìm auditoriumId qua tên:", err);
        }
    }


    // 🧩 Mapping ghế: chuyển số -> mã (A10, B3,…)
    let seatNames = [];
    try {
        if (auditoriumId && ticket.seats?.length) {
            const allSeats = await seatApi.getByAuditorium(auditoriumId);
            seatNames = ticket.seats.map(num => {
                const seat = allSeats.find(s =>
                    s.seatNumber == num || s.seatID == num || s.id == num
                );
                return seat ? `${seat.seatRow}${seat.seatNumber}` : num;
            });
        } else {
            seatNames = ticket.seats || [];
        }
    } catch (err) {
        console.warn("⚠️ Không thể tải danh sách ghế:", err);
        seatNames = ticket.seats || [];
    }


    // 🧾 Hiển thị thông tin vé
    ticketInfo.innerHTML = `
       <div class="mb-2"><span class="info-label">🎬 Phim:</span> <span class="info-value">${ticket.movieTitle}</span></div>
       <div class="mb-2"><span class="info-label">🏢 Chi nhánh:</span> <span class="info-value">${branchName}</span></div>
       <div class="mb-2"><span class="info-label">🏟️ Phòng chiếu:</span> <span class="info-value">${ticket.auditoriumName}</span></div>
       <div class="mb-2"><span class="info-label">🗣️ Ngôn ngữ:</span> <span class="info-value">${ticket.language}</span></div>
       <div class="mb-2"><span class="info-label">⏰ Giờ bắt đầu:</span> <span class="info-value">${formatDate(ticket.startTime)}</span></div>
       <div class="mb-2"><span class="info-label">🎟️ Ghế:</span> <span class="info-value">${seatNames.join(", ") || "Không có"}</span></div>
       <div class="mb-2"><span class="info-label">🍿 Combo:</span> <span class="info-value">${ticket.combos?.join(", ") || "Không có"}</span></div>
       <hr/>
       <div class="mb-2"><span class="info-label">💰 Tổng tiền:</span> <span class="info-value">${formatCurrency(ticket.totalPrice)}</span></div>
       <div class="mb-2"><span class="info-label">💳 Thanh toán:</span> <span class="info-value">${ticket.paymentMethod}</span></div>
       <div class="mb-2"><span class="info-label">📄 Trạng thái vé:</span> <span class="info-value">${ticket.ticketStatus}</span></div>
   `;
}




/* ===================== FORMAT HỖ TRỢ ===================== */
function formatDate(isoStr) {
    if (!isoStr) return "—";
    const d = new Date(isoStr);
    return d.toLocaleString("vi-VN", { hour12: false });
}


function formatCurrency(vnd) {
    if (vnd == null) return "—";
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd);
}

