import { API_BASE_URL, handleResponse, getValidToken } from "./api.js";
import { seatApi } from "./api/seatApi.js";

const form = document.getElementById("otp-form");
const otpInput = document.getElementById("otpCode");
const resultCard = document.getElementById("result-card");
const ticketInfo = document.getElementById("ticket-info");
const scanBtn = document.getElementById("scanQrBtn");
const qrReaderDiv = document.getElementById("qr-reader");

let qrScanner = null;
let qrIsRunning = false;
let lastDecodedAt = 0;

/* ===================== LẤY OTP TỪ QR ===================== */
function extractOtpFromText(text) {
    if (!text) return null;
    const plain = text.trim();
    if (/^\d{6}$/.test(plain)) return plain;

    try {
        const url = new URL(text);
        const qp = url.searchParams.get("otp") || url.searchParams.get("code");
        if (/^\d{6}$/.test(qp || "")) return qp;
    } catch {}

    const m = text.match(/(?:otp|code)\s*[:=]\s*(\d{6})/i);
    if (m) return m[1];

    try {
        const obj = JSON.parse(text);
        const candidate = obj?.otp ?? obj?.data?.otp ?? obj?.code;
        if (/^\d{6}$/.test(String(candidate || ""))) return String(candidate);
    } catch {}

    return null;
}

/* ===================== QUÉT QR ===================== */
async function startQr() {
    if (qrIsRunning) return;
    if (!window.Html5Qrcode) {
        Swal.fire({
            icon: "error",
            title: "Thiếu thư viện",
            text: "Không tìm thấy html5-qrcode!",
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
            }
        );
    } catch (e) {
        qrIsRunning = false;
        qrReaderDiv.style.display = "none";
        Swal.fire({
            icon: "error",
            title: "Không thể mở camera!",
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

/* ===================== SUBMIT OTP ===================== */
form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const code = otpInput.value.trim();

    if (code.length !== 6) {
        Swal.fire({
            icon: "warning",
            title: "OTP sai",
            text: "Vui lòng nhập đúng 6 số!",
        });
        return;
    }

    const token = getValidToken();
    if (!token) {
        Swal.fire({
            icon: "warning",
            title: "Thiếu quyền",
            text: "Bạn phải đăng nhập để kiểm tra OTP!",
        });
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/otp/check`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
            body: JSON.stringify({ code }),
        });

        const data = await handleResponse(res);
        await stopQr();

        Swal.fire({
            icon: "success",
            title: "Vé hợp lệ!",
        });

        await showTicket(data);

    } catch (err) {
        Swal.fire({
            icon: "error",
            title: "Sai OTP",
            text: err.message || "Không xác thực được!",
        });
        resultCard.style.display = "none";
    }
});

/* ===================== HIỂN THỊ VÉ ===================== */
async function showTicket(ticket) {
    resultCard.style.display = "block";

    window.ticketCache = ticket;   // lưu để in PDF

    let auditoriumId = ticket.auditoriumId;
    let branchName = ticket.branchName || "";

    let seatNames = [];

    try {
        if (auditoriumId && ticket.seats?.length) {
            const allSeats = await seatApi.getByAuditorium(auditoriumId);

            seatNames = ticket.seats.map(num => {
                const s = allSeats.find(x =>
                    x.seatNumber == num || x.seatID == num || x.id == num
                );
                return s ? `${s.seatRow}${s.seatNumber}` : num;
            });
        } else {
            seatNames = ticket.seats || [];
        }
    } catch {
        seatNames = ticket.seats || [];
    }

    ticketInfo.innerHTML = `
        <div><b>🎬 Phim:</b> ${ticket.movieTitle}</div>
        <div><b>🏢 Chi nhánh:</b> ${branchName}</div>
        <div><b>🏟️ Phòng chiếu:</b> ${ticket.auditoriumName}</div>
        <div><b>🗣️ Ngôn ngữ:</b> ${ticket.language}</div>
        <div><b>⏰ Giờ bắt đầu:</b> ${formatDate(ticket.startTime)}</div>
        <div><b>🎟️ Ghế:</b> ${seatNames.join(", ")}</div>
        <div><b>🍿 Combo:</b> ${ticket.combos?.join(", ") || "Không có"}</div>
        <hr/>
        <div><b>💰 Tổng tiền:</b> ${formatCurrency(ticket.totalPrice)}</div>
        <div><b>💳 Thanh toán:</b> ${ticket.paymentMethod}</div>
        <div><b>📄 Trạng thái vé:</b> ${ticket.ticketStatus}</div>
    `;

    document.getElementById("btn-print-ticket").style.display = "block";
}

/* ===================== IN PDF ===================== */
document.getElementById("btn-print-ticket")?.addEventListener("click", async () => {

    Swal.fire({
        title: "Đang tạo PDF...",
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    try {
        const seatNames = (window.ticketCache?.seats || []).join(", ");

        const start = new Date(window.ticketCache?.startTime);
        const showDate = start.toLocaleDateString("vi-VN");
        const showTime = start.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", hour12: false });

        const token = getValidToken();

        const req = {

            ticketId: window.ticketCache?.ticketId,

            movieTitle: window.ticketCache?.movieTitle,
            branchName: window.ticketCache?.branchName,
            auditoriumName: window.ticketCache?.auditoriumName,
            showDate,
            showTime,
            seat: seatNames,
            price: window.ticketCache?.totalPrice + "",
            paymentMethod: window.ticketCache?.paymentMethod,
            transactionTime: new Date().toLocaleString("vi-VN"),
            combos: window.ticketCache?.combos || []   //  👈 THÊM DÒNG NÀY


        };

        const pdfRes = await fetch(`${API_BASE_URL}/ticket/print`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`  // 🔥 BẮT BUỘC
            },
            body: JSON.stringify(req)
        });

        const blob = await pdfRes.blob();
        const url = URL.createObjectURL(blob);

        const a = document.createElement("a");
        a.href = url;
        a.download = "ticket.pdf";
        a.click();

        URL.revokeObjectURL(url);
        Swal.close();

    } catch (err) {
        Swal.fire({
            icon: "error",
            title: "Không tạo được PDF",
            text: err.message,
        });
    }
});

/* ===================== FORMAT ===================== */
function formatDate(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("vi-VN", { hour12: false });
}

function formatCurrency(vnd) {
    if (vnd == null) return "—";
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd);
}