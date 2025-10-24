// ==============================
// 💼 CineMaster • Staff Shift
// ==============================
import { staffShiftApi } from './api/staff-shiftApi.js';


const el = {};


document.addEventListener('DOMContentLoaded', init);


/* ============================================================
  🚀 KHỞI TẠO
============================================================ */
async function init() {
    el.openForm = document.getElementById('open-shift');
    el.report = document.getElementById('report-shift');
    el.closeForm = document.getElementById('close-shift');
    el.inputOpen = document.getElementById('openingCash');
    el.inputClose = document.getElementById('closingCash');
    el.reportBody = document.getElementById('report-body');
    el.openLabel = document.getElementById('openAmountLabel');
    el.expectedLabel = document.getElementById('expectedLabel');


    await loadReport();
}


/* ============================================================
  🟢 MỞ CA
============================================================ */
export async function openShift() {
    const openingCash = el.inputOpen.value;
    if (!openingCash) return alert('Vui lòng nhập số tiền đầu ca.');


    try {
        await staffShiftApi.openShift(openingCash);
        alert('✅ Mở ca thành công!');
        el.openForm.classList.add('d-none');
        await loadReport();
    } catch (err) {
        console.error(err);
        alert('❌ Không thể mở ca: ' + (err.message || 'Lỗi hệ thống.'));
    }
}


/* ============================================================
  📊 BÁO CÁO DOANH THU CA HIỆN TẠI
============================================================ */
export async function loadReport() {
    try {
        const data = await staffShiftApi.getReport();
        renderReport(data);
        el.report.classList.remove('d-none');
        el.openForm.classList.add('d-none');
        el.closeForm.classList.add('d-none');
    } catch (err) {
        console.warn('⚠️ Chưa có ca làm đang mở → hiển thị form mở ca');
        el.openForm.classList.remove('d-none');
        el.report.classList.add('d-none');
        el.closeForm.classList.add('d-none');
    }
}


/* ============================================================
  🧾 HIỂN THỊ BẢNG DOANH THU
============================================================ */
function renderReport(data) {
    if (!data) return;


    document.getElementById("openingCashLabel").textContent = formatVND(data.openingCash);
    document.getElementById("soldSeatsLabel").textContent = data.soldSeats;
    document.getElementById("ticketRevenueLabel").textContent = formatVND(data.ticketRevenue);
    document.getElementById("soldCombosLabel").textContent = data.soldCombos;
    document.getElementById("comboRevenueLabel").textContent = formatVND(data.comboRevenue);
    document.getElementById("discountTotalLabel").textContent = formatVND(data.discountTotal);
    document.getElementById("revenueCashLabel").textContent = formatVND(data.revenueCash);
    document.getElementById("revenueTransferLabel").textContent = formatVND(data.revenueTransfer);


    const total = (data.revenueCash || 0) + (data.revenueTransfer || 0);
    document.getElementById("totalRevenueLabel").textContent = formatVND(total);
}




/* ============================================================
  🔴 CHUYỂN SANG FORM KẾT CA
============================================================ */
export async function showCloseForm() {
    try {
        const data = await staffShiftApi.getReport();


        el.openLabel.textContent = formatVND(data.openingCash);
        el.expectedLabel.textContent = formatVND((data.revenueCash || 0) + (data.openingCash || 0));


        el.report.classList.add('d-none');
        el.closeForm.classList.remove('d-none');
    } catch (err) {
        alert('❌ Không thể tải dữ liệu kết ca.');
    }
}


/* ============================================================
  🏁 KẾT CA
============================================================ */
export async function closeShift() {
    const closingCash = el.inputClose.value;
    if (!closingCash) return alert('Vui lòng nhập tiền mặt thực tế.');


    try {
        const result = await staffShiftApi.closeShift(closingCash);
        const diff = result.difference ?? 0;
        alert(`✅ Kết ca thành công!\nChênh lệch: ${formatVND(diff)}`);
        resetForms();
    } catch (err) {
        alert('❌ Kết ca thất bại: ' + (err.message || 'Lỗi hệ thống.'));
    }
}


/* ============================================================
  🧹 RESET FORM SAU KHI KẾT CA
============================================================ */
function resetForms() {
    el.inputOpen.value = '';
    el.inputClose.value = '';
    el.closeForm.classList.add('d-none');
    el.report.classList.add('d-none');
    el.openForm.classList.remove('d-none');
}


/* ============================================================
  💰 HÀM FORMAT VND
============================================================ */
function formatVND(num) {
    if (num == null) return '0 ₫';
    return Number(num).toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
}

