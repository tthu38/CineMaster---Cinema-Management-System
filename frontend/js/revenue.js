// ==========================
// 📊 Revenue Page Script
// ==========================
import { revenueApi } from './api/revenueApi.js';
import { branchApi } from './api/branchApi.js';

const el = {};
let currentScope = 'day';
let role = localStorage.getItem('role');
let branchId = localStorage.getItem('branchId');

document.addEventListener('DOMContentLoaded', init);

/* ============================================================
  🟦 Khởi tạo trang
============================================================ */
async function init() {
    el.scope = document.getElementById('scope');
    el.date = document.getElementById('date');
    el.year = document.getElementById('year');
    el.tableBody = document.getElementById('revenueBody');
    el.btnLoad = document.getElementById('btnLoad');
    el.branch = document.getElementById('branch');

    await loadBranches();
    setupScopeOptions();

    if (el.btnLoad) el.btnLoad.addEventListener('click', loadRevenue);
    if (el.date && el.branch && el.scope) await loadRevenue();
}

/* ============================================================
  🟩 Tùy chọn phạm vi
============================================================ */
function setupScopeOptions() {
    const opts = [];

    if (role === 'Admin') {
        opts.push(`<option value="day">Theo ngày (trong tháng)</option>`);
        opts.push(`<option value="month">Theo tháng (trong năm)</option>`);
        opts.push(`<option value="year">Theo năm</option>`);
    } else if (role === 'Manager') {
        opts.push(`<option value="day">Theo ngày</option>`);
    } else {
        opts.push(`<option value="day">Theo ngày (cá nhân)</option>`);
    }

    el.scope.innerHTML = opts.join('');
    el.scope.addEventListener('change', () => {
        currentScope = el.scope.value;
        toggleInputs();
    });

    toggleInputs();
}

/* ============================================================
  🧩 Ẩn/hiện input theo phạm vi
============================================================ */
function toggleInputs() {
    const dateGroup = document.getElementById('dateGroup');
    const yearGroup = document.getElementById('yearGroup');

    dateGroup.classList.add('d-none');
    yearGroup.classList.add('d-none');

    if (currentScope === 'day') {
        dateGroup.classList.remove('d-none');
        if (el.date) el.date.value = new Date().toISOString().slice(0, 10);
    } else if (currentScope === 'month' || currentScope === 'year') {
        yearGroup.classList.remove('d-none');
        if (el.year) el.year.value = new Date().getFullYear();
    }
}

/* ============================================================
  🏢 Load danh sách chi nhánh
============================================================ */
async function loadBranches() {
    try {
        if (role === 'Manager' && branchId) {
            const branch = await branchApi.getById(branchId);
            el.branch.innerHTML = `<option value="${branch.id || branch.branchID}" selected>
               ${branch.name || branch.branchName || 'Chi nhánh của tôi'}
           </option>`;
            el.branch.disabled = true;
            return;
        }

        if (role === 'Admin') {
            const branches = await branchApi.getAllActive();
            el.branch.innerHTML =
                `<option value="">Tất cả chi nhánh</option>` +
                branches.map(b => `<option value="${b.branchId}">${b.branchName}</option>`).join('');
        } else {
            el.branch.innerHTML = `<option value="${branchId}">Chi nhánh của tôi</option>`;
            el.branch.disabled = true;
        }
    } catch (err) {
        console.error('❌ Lỗi khi tải chi nhánh:', err);
        el.branch.innerHTML = `<option value="">Không tải được danh sách chi nhánh</option>`;
    }
}

/* ============================================================
  💰 Load dữ liệu doanh thu
============================================================ */
async function loadRevenue() {
    try {
        el.tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted">Đang tải dữ liệu...</td></tr>`;
        const date = el.date.value || null;
        const year = el.year.value || null;
        const bId = (el.branch.value && el.branch.value.trim() !== '' && el.branch.value !== 'undefined')
            ? Number(el.branch.value)
            : null;

        let data = [];
        if (currentScope === 'day') data = await revenueApi.getByDay(date, bId);
        else if (currentScope === 'month') data = await revenueApi.getByMonth(year, bId);
        else if (currentScope === 'year') data = await revenueApi.getByYear(year, year, bId);

        renderTable(data);
    } catch (err) {
        console.error('❌ Lỗi loadRevenue:', err);
        alert("Không tải được dữ liệu doanh thu.");
    }
}

/* ============================================================
  📋 Render bảng doanh thu (phiên bản mới)
============================================================ */
function renderTable(rows) {
    if (!rows || rows.length === 0) {
        el.tableBody.innerHTML =
            `<tr><td colspan="9" class="text-center text-muted">Không có dữ liệu</td></tr>`;
        return;
    }

    el.tableBody.innerHTML = rows.map((r, i) => `
       <tr>
           <td>${i + 1}</td>
           <td>${r.label}</td>
           <td>${formatNumber(r.ticketsSold)}</td>
           <td>${formatNumber(r.combosSold)}</td>
           <td>${formatVND(r.grossBeforeDiscount)}</td>
           <td>${formatVND(r.discountTotal)}</td>
           <td>${formatVND(r.revenueOnline)}</td>
           <td>${formatVND(r.revenueCash)}</td>
           <td class="fw-bold text-success">${formatVND(r.totalRevenue)}</td>
       </tr>
   `).join('');
}
/* ============================================================
  💸 Helper format
============================================================ */
function formatVND(num) {
    if (num == null) return '0 ₫';
    return Number(num).toLocaleString('vi-VN', {
        style: 'currency',
        currency: 'VND'
    });
}

function formatNumber(num) {
    if (num == null) return '0';
    return Number(num).toLocaleString('vi-VN');
}


