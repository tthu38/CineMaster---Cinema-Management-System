import { showtimeApi } from './api/showtimeApi.js';
import { screeningPeriodApi } from './api/screeningPeriodApi.js';
import { auditoriumApi } from './api/auditoriumApi.js';
import { branchApi } from './api/branchApi.js';

const ADS_MINUTES = 5;      // Phút quảng cáo đầu phim
const CLEANUP_MINUTES = 15; // Phút dọn rạp sau phim

let modal, el = {}, state = {
    prevPeriodId: null,
    prevAuditoriumId: null,
    submitting: false,
    movieDurationMin: null,
    daySlots: []
};

/* ============================================================
   🔹 KHỞI TẠO MODAL
============================================================ */
export async function initShowtimeCreate({ htmlPath } = {}) {
    if (htmlPath) {
        const html = await fetch(htmlPath).then(r => r.text());
        const wrap = document.createElement('div');
        wrap.innerHTML = html;
        document.body.appendChild(wrap);
    }

    modal = new bootstrap.Modal(document.getElementById('showtimeCreateModal'));

    el = {
        alert: document.getElementById('stcAlert'),
        branch: document.getElementById('stcBranch'),
        period: document.getElementById('stcPeriod'),
        periodHint: document.getElementById('stcPeriodHint'),
        auditorium: document.getElementById('stcAuditorium'),
        language: document.getElementById('stcLanguage'),
        date: document.getElementById('stcDate'),
        start: document.getElementById('stcStart'),
        end: document.getElementById('stcEnd'),
        price: document.getElementById('stcPrice'),
        submit: document.getElementById('stcSubmit'),
        cleanupHint: document.getElementById('stcCleanupHint'),
    };
    el.end.readOnly = true;

    await loadBranches();

    el.branch.addEventListener('change', refreshBranchData);
    el.date.addEventListener('change', refreshBranchData);
    el.period.addEventListener('change', () => { onPeriodChange(); recalcEnd(); });
    el.auditorium.addEventListener('change', async () => {
        await loadDaySlotsForAuditoriumDay();
        recalcEnd();
    });
    el.start.addEventListener('input', recalcEnd);
    el.submit.addEventListener('click', e => { e.preventDefault(); onSubmit(); });
}

/* ============================================================
   🔹 MỞ MODAL TẠO SUẤT CHIẾU
============================================================ */
export function openShowtimeCreate({ defaultDate = null, branchId = null } = {}) {
    showError('');
    el.period.innerHTML = `<option value="">— Chọn period —</option>`;
    el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
    el.periodHint.textContent = '';
    if (el.cleanupHint) el.cleanupHint.textContent = '';

    el.language.value = 'Vietnamese';
    el.date.value = defaultDate || todayYMD();
    el.start.value = '';
    el.end.value = '';
    el.price.value = '120000';

    state.prevPeriodId = null;
    state.prevAuditoriumId = null;
    state.movieDurationMin = null;
    state.daySlots = [];

    const role = localStorage.getItem("role");
    const managerBranch = localStorage.getItem("branchId");

    if (role === "Manager" && managerBranch) {
        // 🔒 Manager: chỉ chi nhánh của họ
        el.branch.innerHTML = `<option value="${managerBranch}" selected>Chi nhánh của tôi</option>`;
        el.branch.disabled = true;
    } else {
        // 🧩 Admin: có thể chọn chi nhánh bất kỳ
        el.branch.disabled = false;
        if (branchId) el.branch.value = String(branchId);
    }

    refreshBranchData().finally(() => modal.show());
}

/* ============================================================
   🔹 TIỆN ÍCH
============================================================ */
function showError(msg) {
    if (!msg) { el.alert.classList.add('d-none'); el.alert.textContent = ''; return; }
    el.alert.textContent = msg;
    el.alert.classList.remove('d-none');
}
function todayYMD() { return new Date().toISOString().slice(0, 10); }
function addDaysYMD(ymd, days) {
    const [y, m, d] = ymd.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    dt.setDate(dt.getDate() + days);
    return dt.toISOString().slice(0, 10);
}
function toISO(dateStr, timeStr) { return `${dateStr}T${timeStr}:00`; }

/* ============================================================
   🔹 LOAD DANH SÁCH CHI NHÁNH
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
            } else {
                el.branch.innerHTML = `<option value="">(Không tải được chi nhánh của bạn)</option>`;
                el.branch.disabled = true;
            }
            return;
        }

        const branches = await branchApi.getAllActive() ?? [];
        el.branch.innerHTML = branches
            .map(b => {
                const id = b.id ?? b.branchId ?? b.branchID;
                const name = b.branchName ?? b.name ?? b.branch ?? "Không tên";
                return `<option value="${id}">${name}</option>`;
            })
            .join('');
        el.branch.disabled = false;

    } catch (err) {
        console.error('Không tải được chi nhánh:', err);
        el.branch.innerHTML = `<option value="">(Không tải được rạp)</option>`;
    }
}

/* ============================================================
   🔹 LOAD PERIOD / AUDITORIUM THEO CHI NHÁNH
   🔸 Chỉ hiển thị phim có khoảng chiếu bao gồm ngày được chọn
============================================================ */
async function refreshBranchData() {
    const branchId = Number(el.branch.value || 0);
    const onDate = el.date.value || todayYMD();

    if (!branchId) {
        showError("Vui lòng chọn chi nhánh trước khi tạo lịch chiếu.");
        return;
    }

    try {
        el.period.innerHTML = `<option value="">— Chọn period —</option>`;
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
        el.periodHint.textContent = '';
        if (el.cleanupHint) el.cleanupHint.textContent = '';
        state.movieDurationMin = null;

        const [periods, auds] = await Promise.all([
            screeningPeriodApi.getByBranch(branchId),
            auditoriumApi.getByBranch(branchId)
        ]);

        // ✅ Lọc chỉ giữ period có ngày chiếu hợp lệ
        const validPeriods = (periods || []).filter(p => {
            const from = getStartDate(p);
            const to = getEndDate(p);
            return onDate >= from && onDate <= to;
        });

        if (validPeriods.length === 0) {
            el.period.innerHTML = `<option disabled selected>Không có phim nào đang chiếu ngày này</option>`;
        } else {
            const periodOptions = validPeriods.map(p => {
                const pid = getPeriodId(p);
                const from = getStartDate(p);
                const to = getEndDate(p);
                const title = getMovieTitle(p);
                const dur = getDuration(p);
                return `<option value="${pid}" data-range="${from}..${to}" ${dur ? `data-duration="${dur}"` : ''}>
                    ${title} (${from} → ${to})
                </option>`;
            }).join('');
            el.period.innerHTML = `<option value="">— Chọn period —</option>${periodOptions}`;
        }

        // 🎬 Load phòng chiếu
        const audOptions = (auds || []).map(a => {
            const aid = getAuditoriumId(a);
            return `<option value="${aid}">${a.name} • ${a.type} • ${a.capacity} ghế</option>`;
        }).join('');
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>${audOptions}`;

        await loadDaySlotsForAuditoriumDay();
        recalcEnd();

    } catch (e) {
        console.error(e);
        showError(e?.message || 'Không tải được dữ liệu period/phòng chiếu.');
    }
}

/* ============================================================
   🔹 HÀM HỖ TRỢ
============================================================ */
const getPeriodId = p => p?.periodId ?? p?.id ?? null;
const getMovieTitle = p => p?.movieTitle ?? p?.movie?.title ?? p?.title ?? 'Unknown';
const getStartDate = p => p?.startDate ?? p?.from ?? '';
const getEndDate = p => p?.endDate ?? p?.to ?? '';
const getDuration = p => p?.duration ?? p?.movie?.duration ?? null;
const getAuditoriumId = a => a?.auditoriumID ?? a?.auditoriumId ?? a?.id ?? null;

function onPeriodChange() {
    const opt = el.period.selectedOptions?.[0];
    const range = opt?.getAttribute('data-range') || '';
    el.periodHint.textContent = range ? `Khoảng hợp lệ: ${range}` : '';

    const [from, to] = range.split('..');
    if (from && to) {
        el.date.min = from;
        el.date.max = to;
        if (el.date.value < from || el.date.value > to) el.date.value = from;
    }

    const durAttr = opt?.getAttribute('data-duration');
    const dur = durAttr ? Number(durAttr) : null;
    state.movieDurationMin = dur ? dur + ADS_MINUTES : null;

    if (dur) {
        el.periodHint.textContent += `${el.periodHint.textContent ? ' • ' : ''}Thời lượng: ${dur} phút (+${ADS_MINUTES}p QC)`;
    }

    recalcEnd();
}

/* ============================================================
   🔹 LOAD SUẤT TRONG NGÀY (để check đệm)
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
   🔹 TÍNH GIỜ KẾT THÚC + ĐỆM
============================================================ */
function hhmmToMinutes(str) {
    if (!str) return null;
    const [h, m] = str.split(':').map(Number);
    return h * 60 + m;
}
function minutesToHHmm(mins) {
    if (mins == null) return '';
    mins = ((mins % 1440) + 1440) % 1440;
    const h = Math.floor(mins / 60), m = mins % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}
function recalcEnd() {
    const startStr = el.start.value;
    const startMin = hhmmToMinutes(startStr);
    if (startMin == null || !state.movieDurationMin) {
        el.end.value = '';
        if (el.cleanupHint) el.cleanupHint.textContent = '';
        return;
    }

    const endMin = startMin + state.movieDurationMin;
    el.end.value = minutesToHHmm(endMin);

    const nextEarliest = minutesToHHmm(endMin + CLEANUP_MINUTES);
    if (el.cleanupHint)
        el.cleanupHint.textContent = `Gợi ý: suất kế tiếp nên bắt đầu không sớm hơn ${nextEarliest} (đệm ${CLEANUP_MINUTES}p).`;
}

/* ============================================================
   🔹 SUBMIT TẠO LỊCH CHIẾU
============================================================ */
async function onSubmit() {
    if (state.submitting) return;
    try {
        showError('');

        const branchId = Number(el.branch.value || 0);
        const periodId = Number(el.period.value);
        const auditoriumId = Number(el.auditorium.value);
        const language = el.language.value;
        const price = Number(el.price.value || 0);
        const date = el.date.value;
        const start = el.start.value;

        if (!el.end.value) recalcEnd();
        const end = el.end.value;

        if (!branchId) throw new Error('Vui lòng chọn chi nhánh');
        if (!periodId) throw new Error('Vui lòng chọn Screening Period');
        if (!auditoriumId) throw new Error('Vui lòng chọn Phòng chiếu');
        if (!date) throw new Error('Vui lòng chọn Ngày');
        if (!start) throw new Error('Vui lòng chọn giờ bắt đầu');
        if (!end) throw new Error('Thiếu giờ kết thúc');

        await loadDaySlotsForAuditoriumDay();
        const startTime = toISO(date, start);
        const endDateForIso = end > start ? date : addDaysYMD(date, 1);
        const endTime = toISO(endDateForIso, end);

        const payload = { periodId, auditoriumId, startTime, endTime, language, price, branchId };

        setSubmitting(true);
        await showtimeApi.create(payload);

        state.prevPeriodId = periodId;
        state.prevAuditoriumId = auditoriumId;
        window.dispatchEvent(new Event('showtime:created'));
        modal.hide();

    } catch (e) {
        showError(e?.message || 'Tạo lịch chiếu thất bại');
    } finally {
        setSubmitting(false);
    }
}

/* ============================================================
   🔹 NÚT SUBMIT LOADING
============================================================ */
function setSubmitting(v) {
    state.submitting = !!v;
    el.submit.disabled = v;
    el.submit.innerHTML = v
        ? `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`
        : `<i class="fa-solid fa-floppy-disk me-2"></i>Lưu`;
}
