// ======================================================
// 🎬 CINE MASTER - SHOWTIME BATCH MODULE
// Version: CineMaster aligned (same logic as showtime-create)
// Author: Giang Nguyen
// ======================================================

import { showtimeApi } from './api/showtimeApi.js';
import { screeningPeriodApi } from './api/screeningPeriodApi.js';
import { auditoriumApi } from './api/auditoriumApi.js';
import { branchApi } from './api/branchApi.js';

const CLEANUP_MINUTES = 15; // phút dọn rạp giữa 2 suất

let modal, el = {}, state = {
    submitting: false,
    movieDurationMin: null,
    daySlots: []
};

/* ============================================================
   🔹 KHỞI TẠO MODAL
============================================================ */
export async function initShowtimeBatch({ htmlPath } = {}) {
    if (htmlPath) {
        const html = await fetch(htmlPath).then(r => r.text());
        const wrap = document.createElement('div');
        wrap.innerHTML = html;
        document.body.appendChild(wrap);
    }

    modal = new bootstrap.Modal(document.getElementById('showtimeBatchModal'));
    el = {
        alert: document.getElementById('stbAlert'),
        branch: document.getElementById('stbBranch'),
        period: document.getElementById('stbPeriod'),
        periodHint: document.getElementById('stbPeriodHint'),
        auditorium: document.getElementById('stbAuditorium'),
        language: document.getElementById('stbLanguage'),
        date: document.getElementById('stbDate'),
        times: document.getElementById('stbTimes'),
        addTimeBtn: document.getElementById('stbAddTime'),
        price: document.getElementById('stbPrice'),
        submit: document.getElementById('stbSubmit'),
        cleanupHint: document.getElementById('stbCleanupHint'),
    };

    el.addTimeBtn.addEventListener('click', addTimeInput);
    el.submit.addEventListener('click', e => { e.preventDefault(); onSubmit(); });

    await loadBranches();

    el.branch.addEventListener('change', refreshBranchData);
    el.date.addEventListener('change', refreshBranchData);
    el.date.addEventListener('change', () => checkIfToday(el.date.value));
    el.period.addEventListener('change', () => { onPeriodChange(); refreshCleanupHint(); });
    el.auditorium.addEventListener('change', async () => { await loadDaySlotsForAuditoriumDay(); });
}

/* ============================================================
   🔹 MỞ MODAL
============================================================ */
export function openShowtimeBatch({ defaultDate = null, branchId = null } = {}) {
    showError('');
    el.period.innerHTML = `<option value="">— Chọn period —</option>`;
    el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
    el.periodHint.textContent = '';
    el.cleanupHint.textContent = '';

    el.language.value = 'Vietnamese';
    el.date.value = defaultDate || todayYMD();
    el.price.value = '120000';
    el.times.innerHTML = '';
    addTimeInput();

    state.movieDurationMin = null;
    state.daySlots = [];

    const role = localStorage.getItem("role");
    const managerBranch = localStorage.getItem("branchId");

    if (role === "Manager" && managerBranch) {
        el.branch.innerHTML = `<option value="${managerBranch}" selected>Chi nhánh của tôi</option>`;
        el.branch.disabled = true;
    } else {
        el.branch.disabled = false;
        if (branchId) el.branch.value = String(branchId);
    }

    // ⚡ Kiểm tra ngày: nếu hôm nay → disable nút tạo
    checkIfToday(el.date.value);

    refreshBranchData().finally(() => modal.show());
}

/* ============================================================
   🔹 DISABLE "TẠO ĐỒNG LOẠT" KHI NGÀY = HÔM NAY
============================================================ */
function checkIfToday(selectedDate) {
    const today = todayYMD();
    if (selectedDate === today) {
        el.submit.disabled = true;
        el.submit.classList.add('btn-secondary');
        el.submit.classList.remove('btn-info');
        el.submit.innerHTML = `<i class="fa-solid fa-ban me-2"></i>Không thể tạo suất cho hôm nay`;
        showError("⚠️ Không thể tạo lịch chiếu cho ngày hôm nay.");
    } else {
        el.submit.disabled = false;
        el.submit.classList.remove('btn-secondary');
        el.submit.classList.add('btn-info');
        el.submit.innerHTML = `<i class="fa-solid fa-floppy-disk me-2"></i>Tạo đồng loạt`;
        showError('');
    }
}

/* ============================================================
   🔹 LOAD CHI NHÁNH
============================================================ */
async function loadBranches() {
    try {
        const role = localStorage.getItem("role");
        const branchId = localStorage.getItem("branchId");

        if (role === "Manager" && branchId) {
            const branch = await branchApi.getById(branchId);
            if (branch) {
                el.branch.innerHTML = `<option value="${branch.id}" selected>${branch.branchName}</option>`;
                el.branch.disabled = true;
            }
            return;
        }

        const branches = await branchApi.getAllActive() ?? [];
        el.branch.innerHTML = branches.map(b => {
            const id = b.id ?? b.branchId ?? b.branchID;
            const name = b.branchName ?? b.name ?? b.branch ?? "Không tên";
            return `<option value="${id}">${name}</option>`;
        }).join('');
        el.branch.disabled = false;

    } catch (err) {
        console.error('⚠️ Không tải được chi nhánh:', err);
        el.branch.innerHTML = `<option value="">(Không tải được rạp)</option>`;
    }
}

/* ============================================================
   🔹 LOAD PERIOD & PHÒNG CHIẾU
============================================================ */
/* ============================================================
   🔹 LOAD PERIOD & PHÒNG CHIẾU THEO CHI NHÁNH
   🔸 ĐÃ FIX: tự set duration + tính end time ngay khi mở modal
============================================================ */
async function refreshBranchData() {
    const branchId = Number(el.branch.value || 0);
    const onDate = el.date.value || todayYMD();
    if (!branchId) return;

    try {
        // Reset UI
        el.period.innerHTML = `<option value="">— Chọn period —</option>`;
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
        el.periodHint.textContent = '';
        el.cleanupHint.textContent = '';
        state.movieDurationMin = null;

        // 🔹 Gọi API song song
        const [periods, auds] = await Promise.all([
            screeningPeriodApi.getByBranch(branchId),
            auditoriumApi.getByBranch(branchId)
        ]);

        // 🔹 Lọc các period có ngày chiếu hợp lệ với ngày được chọn
        const validPeriods = (periods || []).filter(p => {
            const from = p.startDate ?? p.from ?? '';
            const to = p.endDate ?? p.to ?? '';
            return onDate >= from && onDate <= to;
        });

        // 🔹 Đổ dropdown Period
        if (validPeriods.length === 0) {
            el.period.innerHTML = `<option disabled selected>Không có phim nào đang chiếu ngày này</option>`;
        } else {
            el.period.innerHTML = validPeriods.map(p => {
                const pid = p.periodId ?? p.id;
                const from = p.startDate ?? p.from;
                const to = p.endDate ?? p.to;
                const title = p.movieTitle ?? p.movie?.title ?? 'Unknown';
                const dur = p.duration ?? p.movie?.duration ?? null;
                return `<option value="${pid}" data-duration="${dur}" data-range="${from}..${to}">
                    ${title} (${from} → ${to})
                </option>`;
            }).join('');
        }

        // 🔹 Đổ dropdown Phòng chiếu
        el.auditorium.innerHTML = (auds || []).map(a => {
            const aid = a.auditoriumID ?? a.id;
            return `<option value="${aid}">${a.name} • ${a.type} • ${a.capacity} ghế</option>`;
        }).join('');

        // 🔹 Load suất chiếu trong ngày (nếu cần kiểm tra trùng giờ)
        await loadDaySlotsForAuditoriumDay();

        // ✅ FIX QUAN TRỌNG: Gọi onPeriodChange() ngay khi có period đầu tiên
        // Giúp set state.movieDurationMin → auto nhảy giờ end khi chọn start
        if (el.period.options.length > 0) {
            // Nếu chưa chọn gì thì chọn option đầu tiên
            if (!el.period.value || el.period.value === '') {
                el.period.selectedIndex = 0;
            }
            onPeriodChange(); // ⚡ Gọi để set duration + hint cleanup
        }

    } catch (err) {
        console.error('⚠ Lỗi load tên phim/phòng chiếu:', err);
        showError(err.message || "Không thể tải dữ liệu tên phim/phòng chiếu.");
    }
}


/* ============================================================
   🔹 XỬ LÝ PERIOD
============================================================ */
function onPeriodChange() {
    const opt = el.period.selectedOptions?.[0];
    const durAttr = opt?.getAttribute('data-duration');
    const range = opt?.getAttribute('data-range');
    const dur = durAttr ? Number(durAttr) : null;
    state.movieDurationMin = dur;

    el.periodHint.textContent = range ? `Khoảng hợp lệ: ${range}` : '';
    if (dur) el.periodHint.textContent += ` • Thời lượng: ${dur} phút`;

    refreshCleanupHint();
}

/* ============================================================
   🔹 GỢI Ý BUFFER
============================================================ */
function refreshCleanupHint() {
    if (!state.movieDurationMin) {
        el.cleanupHint.textContent = '';
        return;
    }
    el.cleanupHint.textContent = `Thời lượng phim: ${state.movieDurationMin} phút (+${CLEANUP_MINUTES}p dọn rạp)`;
}

/* ============================================================
   🔹 LOAD SUẤT CHIẾU TRONG NGÀY (nếu cần check overlap)
============================================================ */
async function loadDaySlotsForAuditoriumDay() {
    const auditoriumRaw = el.auditorium.value;
    const date = el.date.value;
    if (!auditoriumRaw || !date) { state.daySlots = []; return; }

    const from = `${date}T00:00:00`;
    const to = `${addDaysYMD(date, 1)}T00:00:00`;

    const resp = await showtimeApi.search({
        auditoriumId: Number(auditoriumRaw),
        from, to, page: 0, size: 200, sort: 'startTime,asc'
    });
    state.daySlots = resp?.content || [];
}

/* ============================================================
   🔹 TẠO INPUT GIỜ CHIẾU
============================================================ */
function addTimeInput() {
    const group = document.createElement('div');
    group.className = 'd-flex align-items-center gap-2 mb-2';

    const startInput = document.createElement('input');
    startInput.type = 'time';
    startInput.className = 'form-control w-auto';
    startInput.step = 60;

    const endInput = document.createElement('input');
    endInput.type = 'time';
    endInput.className = 'form-control w-auto';
    endInput.step = 60;
    endInput.readOnly = true;

    startInput.addEventListener('change', () => {
        if (!state.movieDurationMin) return;
        const [h, m] = startInput.value.split(':').map(Number);
        const startDate = new Date(2000, 0, 1, h, m);
        const endDate = new Date(startDate.getTime() + state.movieDurationMin * 60000);
        const nextStartDate = new Date(endDate.getTime() + CLEANUP_MINUTES * 60000);

        // Cập nhật giờ kết thúc suất
        const hh = String(endDate.getHours()).padStart(2, '0');
        const mm = String(endDate.getMinutes()).padStart(2, '0');
        endInput.value = `${hh}:${mm}`;

        // Gợi ý suất kế tiếp nên bắt đầu sau bao lâu
        startInput.dataset.nextRecommended = `${String(nextStartDate.getHours()).padStart(2, '0')}:${String(nextStartDate.getMinutes()).padStart(2, '0')}`;
    });


    group.appendChild(startInput);
    group.appendChild(endInput);
    el.times.appendChild(group);
}
/* ============================================================
   🔹 SUBMIT CÓ KIỂM TRA TRÙNG GIỜ & KHOẢNG CÁCH 15 PHÚT
============================================================ */
async function onSubmit() {
    if (state.submitting) return;
    try {
        showError('');

        const branchId = Number(el.branch.value);
        const periodId = Number(el.period.value);
        const auditoriumId = Number(el.auditorium.value);
        const language = el.language.value;
        const price = Number(el.price.value);
        const date = el.date.value;

        if (!branchId || !periodId || !auditoriumId)
            throw new Error('Thiếu thông tin bắt buộc.');

        // 🕐 Lấy tất cả các cặp start - end
        const rows = el.times.querySelectorAll('div');
        const timePairs = [];
        for (const row of rows) {
            const [start, end] = row.querySelectorAll('input[type="time"]');
            if (start?.value && end?.value)
                timePairs.push({ start: start.value, end: end.value });
        }

        if (timePairs.length === 0)
            throw new Error("Vui lòng nhập ít nhất 1 khung giờ chiếu.");

        // 🔍 Sort các suất theo giờ bắt đầu để kiểm tra cách nhau
        const sorted = timePairs.sort((a, b) => a.start.localeCompare(b.start));

        // 🔹 Check khoảng cách giữa các suất trong cùng batch
        for (let i = 0; i < sorted.length - 1; i++) {
            const currentEnd = hhmmToMinutes(sorted[i].end);
            const nextStart = hhmmToMinutes(sorted[i + 1].start);
            if (nextStart - currentEnd < CLEANUP_MINUTES) {
                const msg = `❌ Suất ${i + 1} và suất ${i + 2} cách nhau < ${CLEANUP_MINUTES} phút!`;
                showError(msg);
                return; // ❌ Dừng không gửi API
            }
        }

        // 🧩 Load danh sách suất chiếu hiện có trong ngày để check trùng giờ
        const from = `${date}T00:00:00`;
        const to = `${date}T23:59:59`;
        const existing = await showtimeApi.search({
            auditoriumId, from, to, page: 0, size: 200, sort: 'startTime,asc'
        });
        const existingSlots = (existing?.content || []).map(s => ({
            start: new Date(s.startTime),
            end: new Date(s.endTime)
        }));

        // 🔍 Kiểm tra từng suất mới có trùng suất cũ không
        for (const { start, end } of sorted) {
            const startISO = new Date(`${date}T${start}:00`);
            const endISO = new Date(`${date}T${end}:00`);
            for (const slot of existingSlots) {
                const overlap = startISO < slot.end && endISO > slot.start;
                if (overlap) {
                    const msg = `Trùng suất chiếu với phim khác trong cùng phòng (${slot.start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})`;
                    showError(msg);
                    return; // ❌ Dừng không gửi API
                }
            }
        }

        // ✅ Nếu mọi thứ hợp lệ → Gửi API tạo
        setSubmitting(true);

        for (const { start, end } of sorted) {
            const startTime = `${date}T${start}:00`;
            const endTime = `${date}T${end}:00`;
            const payload = { periodId, auditoriumId, startTime, endTime, language, price, branchId };
            try {
                await showtimeApi.create(payload);
            } catch (err) {
                // ✅ Hiển thị message cụ thể từ backend nếu có
                const backendMsg = err.message || '';
                let msg = backendMsg;

                if (err.status === 409 && backendMsg.includes("Suất chiếu")) {
                    msg = ` ${backendMsg}`; // Giữ nguyên message chi tiết
                } else if (err.status === 409) {
                    msg = "Suất chiếu bị trùng trong khung giờ đã chọn.";
                } else if (err.status === 403) {
                    msg = "Bạn không có quyền tạo lịch chiếu.";
                } else if (err.status === 401) {
                    msg = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
                } else if (err.status === 500) {
                    msg = backendMsg || "Lỗi hệ thống khi tạo lịch chiếu.";
                }

                // 👉 hiển thị thông báo tại chỗ
                showError(msg);
                console.warn("⚠️ Lỗi tạo suất:", err);
                return; // dừng lại, không gửi tiếp
            }

        }

        modal.hide();
        window.dispatchEvent(new Event('showtime:created'));

    } catch (e) {
        showError(e?.message || 'Tạo đồng loạt thất bại');
    } finally {
        setSubmitting(false);
    }
}

/* ============================================================
   🔹 HÀM PHỤ CHUYỂN HH:mm → phút
============================================================ */
function hhmmToMinutes(str) {
    if (!str) return 0;
    const [h, m] = str.split(':').map(Number);
    return h * 60 + m;
}


/* ============================================================
   🔹 TIỆN ÍCH CHUNG
============================================================ */
function showError(msg) {
    if (!msg) { el.alert.classList.add('d-none'); el.alert.textContent = ''; return; }
    el.alert.textContent = msg;
    el.alert.classList.remove('d-none');
}

function setSubmitting(v) {
    state.submitting = !!v;
    el.submit.disabled = v;
    el.submit.innerHTML = v
        ? `<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...`
        : `<i class="fa-solid fa-floppy-disk me-2"></i>Tạo đồng loạt`;
}

function todayYMD() { return new Date().toISOString().slice(0, 10); }
function addDaysYMD(ymd, days) {
    const [y, m, d] = ymd.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    dt.setDate(dt.getDate() + days);
    return dt.toISOString().slice(0, 10);
}
