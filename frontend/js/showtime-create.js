// js/showtime-create.js
import { showtimeApi, screeningPeriodApi, auditoriumApi } from './api.js';

const ADS_MINUTES = 5;      // phút quảng cáo đầu phim
const CLEANUP_MINUTES = 15; // đệm dọn rạp

let modal, el = {}, state = {
    prevPeriodId: null,
    prevAuditoriumId: null,
    submitting: false,
    movieDurationMin: null, // phút (đã cộng quảng cáo khi set)
    daySlots: []            // cache các suất trong ngày của phòng đã chọn
};

export async function initShowtimeCreate({ htmlPath } = {}) {
    // nạp HTML modal (nếu chưa có sẵn trong trang)
    if (htmlPath) {
        const html = await fetch(htmlPath).then(r => r.text());
        const wrap = document.createElement('div');
        wrap.innerHTML = html;
        document.body.appendChild(wrap);
    }

    modal = new bootstrap.Modal(document.getElementById('showtimeCreateModal'));

    el.alert        = document.getElementById('stcAlert');
    el.branch       = document.getElementById('stcBranch');
    el.period       = document.getElementById('stcPeriod');
    el.periodHint   = document.getElementById('stcPeriodHint');
    el.auditorium   = document.getElementById('stcAuditorium');
    el.language     = document.getElementById('stcLanguage');
    el.date         = document.getElementById('stcDate');
    el.start        = document.getElementById('stcStart');
    el.end          = document.getElementById('stcEnd');
    el.price        = document.getElementById('stcPrice');
    el.submit       = document.getElementById('stcSubmit');
    el.cleanupHint  = document.getElementById('stcCleanupHint');

    el.end.readOnly = true; // giờ kết thúc tự tính

    await loadBranches();

    // Events
    el.branch.addEventListener('change', async () => {
        await onBranchChange();
        await loadDaySlotsForAuditoriumDay(); // sau khi render lại phòng/period
        recalcEnd();
    });

    el.date.addEventListener('change', async () => {
        await onBranchChange();               // reload period theo ngày
        await loadDaySlotsForAuditoriumDay(); // tải suất trong ngày mới
        recalcEnd();
    });

    el.period.addEventListener('change', () => {
        onPeriodChange();                     // cập nhật min/max + duration
        recalcEnd();
    });

    el.auditorium.addEventListener('change', async () => {
        await loadDaySlotsForAuditoriumDay(); // đổi phòng -> tải lại slots
        recalcEnd();
    });

    el.start.addEventListener('input', recalcEnd);        // đổi giờ bắt đầu -> auto end + hint tiếp theo

    // Chặn submit mặc định
    el.submit.addEventListener('click', (e) => {
        e?.preventDefault?.();
        onSubmit();
    });
}

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

    if (branchId) el.branch.value = String(branchId);

    onBranchChange()
        .then(loadDaySlotsForAuditoriumDay)
        .then(recalcEnd)
        .finally(() => modal.show());
}

/* ================= Helpers ================= */
function addDaysYMD(ymd, days){
    const [y,m,d] = ymd.split('-').map(Number);
    const dt = new Date(y, m-1, d);
    dt.setDate(dt.getDate() + days);
    const yy = dt.getFullYear();
    const mm = String(dt.getMonth()+1).padStart(2,'0');
    const dd = String(dt.getDate()).padStart(2,'0');
    return `${yy}-${mm}-${dd}`;
}

function showError(msg) {
    if (!msg) { el.alert.classList.add('d-none'); el.alert.textContent = ''; return; }
    el.alert.textContent = msg;
    el.alert.classList.remove('d-none');
}
function todayYMD() {
    const d = new Date();
    const m = d.getMonth() + 1, day = d.getDate();
    return `${d.getFullYear()}-${String(m).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}
function toISO(dateStr, timeStr) {
    return `${dateStr}T${timeStr}:00`;
}
// demo: thay bằng API branch thực nếu có
async function loadBranches() {
    const branches = [
        { id: 1, name: 'Branch Hanoi' },
        { id: 2, name: 'Branch HCM' },
        { id: 3, name: 'Branch Da Nang' },
    ];
    el.branch.innerHTML = branches.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
}
// Chuẩn hoá field từ BE
function getPeriodId(p){ return p?.periodId ?? p?.id ?? p?.screeningPeriodId ?? p?.screeningPeriodID ?? null; }
function getMovieTitle(p){ return p?.movieTitle ?? p?.movie?.title ?? p?.title ?? 'Unknown'; }
function getStartDate(p){ return p?.startDate ?? p?.start ?? p?.from ?? ''; }
function getEndDate(p){ return p?.endDate ?? p?.end ?? p?.to ?? ''; }
function getDuration(p){ return p?.duration ?? p?.movieDuration ?? p?.movie?.duration ?? null; }
function getAuditoriumId(a){ return a?.auditoriumId ?? a?.auditoriumID ?? a?.id ?? null; }

function hhmmToMinutes(str){
    if(!str) return null;
    const [h,m] = str.split(':').map(Number);
    if (Number.isNaN(h) || Number.isNaN(m)) return null;
    return h*60 + m;
}
function minutesToHHmm(mins){
    if (mins == null) return '';
    mins = ((mins % 1440) + 1440) % 1440; // 0..1439
    const h = Math.floor(mins / 60), m = mins % 60;
    return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
}

/* ================= Data Loading ================= */
async function onBranchChange() {
    const branchId = Number(el.branch.value || 0) || null;
    const onDate = el.date.value || todayYMD();

    try {
        el.period.innerHTML = `<option value="">— Chọn period —</option>`;
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
        el.periodHint.textContent = '';
        if (el.cleanupHint) el.cleanupHint.textContent = '';
        state.movieDurationMin = null;

        const [periods, auds] = await Promise.all([
            screeningPeriodApi.active({ branchId, onDate }),
            auditoriumApi.listByBranch(branchId),
        ]);

        // render Period + data-duration
        const periodOptions = (periods || []).map(p => {
            const pid   = getPeriodId(p);
            const from  = getStartDate(p);
            const to    = getEndDate(p);
            const title = getMovieTitle(p);
            const dur   = getDuration(p); // phút
            return `<option value="${pid ?? ''}"
                      data-range="${from}..${to}"
                      ${dur != null ? `data-duration="${dur}"` : ''}>
                ${title} (${from} → ${to})
              </option>`;
        }).join('');
        el.period.innerHTML = `<option value="">— Chọn period —</option>${periodOptions}`;

        // render Auditorium
        const audOptions = (auds || []).map(a => {
            const aid = getAuditoriumId(a);
            return `<option value="${aid}">
                ${a.name} • ${a.type} • ${a.capacity} ghế
              </option>`;
        }).join('');
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>${audOptions}`;

        // Auto-select nếu chỉ có 1 period
        if ((periods?.length || 0) === 1) {
            const pid = getPeriodId(periods[0]);
            if (pid != null) {
                el.period.value = String(pid);
                onPeriodChange();
            }
        }
        // Giữ lại lựa chọn cũ nếu còn
        if (state.prevPeriodId && [...el.period.options].some(o => o.value === String(state.prevPeriodId))) {
            el.period.value = String(state.prevPeriodId);
            onPeriodChange();
        }
        if (state.prevAuditoriumId && [...el.auditorium.options].some(o => o.value === String(state.prevAuditoriumId))) {
            el.auditorium.value = String(state.prevAuditoriumId);
        }

    } catch (e) {
        console.error(e);
        showError(e?.message || 'Không tải được dữ liệu period/phòng chiếu');
    }
}

function onPeriodChange() {
    const opt = el.period.selectedOptions?.[0];
    const range = opt?.getAttribute('data-range') || '';
    el.periodHint.textContent = range ? `Khoảng hợp lệ: ${range}` : '';

    const [from, to] = range.split('..');
    if (from && to) {
        el.date.min = from;
        el.date.max = to;
        if (el.date.value && (el.date.value < from || el.date.value > to)) el.date.value = from;
    } else {
        el.date.removeAttribute('min');
        el.date.removeAttribute('max');
    }

    // lấy duration → lưu (đã cộng ADS)
    const durAttr = opt?.getAttribute('data-duration');
    const dur = durAttr ? Number(durAttr) : null;
    state.movieDurationMin = (dur != null && !Number.isNaN(dur)) ? (dur + ADS_MINUTES) : null;

    // hint thêm thời lượng
    if (dur != null && !Number.isNaN(dur)) {
        el.periodHint.textContent += `${el.periodHint.textContent ? ' • ' : ''}Thời lượng: ${dur} phút (+${ADS_MINUTES}p QC)`;
    }

    recalcEnd();
}

/* ================= Buffer helpers ================= */

// Tải danh sách suất trong ngày của phòng đang chọn (để check buffer trên FE)
async function loadDaySlotsForAuditoriumDay() {
    const auditoriumRaw = el.auditorium.value;
    const date = el.date.value;
    if (!auditoriumRaw || !date) { state.daySlots = []; return; }

    // from: 00:00 ngày đó; to: 00:00 ngày hôm sau
    const from = `${date}T00:00:00`;
    const d = new Date(date + 'T00:00:00');
    const next = new Date(d.getTime() + 24*60*60*1000);
    const to = `${next.getFullYear()}-${String(next.getMonth()+1).padStart(2,'0')}-${String(next.getDate()).padStart(2,'0')}T00:00:00`;

    const resp = await showtimeApi.search({
        auditoriumId: Number(auditoriumRaw),
        from, to, page: 0, size: 200, sort: 'startTime,asc'
    });
    state.daySlots = resp?.content || [];
}

// Kiểm tra va chạm buffer 15' với các suất đã có (cùng ngày/cùng phòng)
function violatesBuffer(startStr, endStr) {
    if (!state.daySlots?.length) return false;

    const [sh, sm] = startStr.split(':').map(Number);
    const [eh, em] = endStr.split(':').map(Number);
    const startMin = sh*60 + sm;
    const endMin   = eh*60 + em;

    // parse "YYYY-MM-DDTHH:mm:ss" -> phút trong ngày (bỏ timezone khi so sánh nội bộ)
    const toDayMinutes = (iso) => {
        const t = iso.split('T')[1]?.slice(0,5) || '00:00';
        const [h,m] = t.split(':').map(Number);
        return h*60 + m;
    };

    for (const s of state.daySlots) {
        const sStart = toDayMinutes(s.startTime);
        const sEnd   = toDayMinutes(s.endTime);
        // Vi phạm nếu giao nhau khi mở rộng hai phía: start-15 .. end+15
        if (sStart < (endMin + CLEANUP_MINUTES) && sEnd > (startMin - CLEANUP_MINUTES)) {
            return true;
        }
    }
    return false;
}

/* ================= Auto end & hint next slot ================= */
function recalcEnd(){
    const startStr = el.start.value;
    const startMin = hhmmToMinutes(startStr);
    if (startMin == null) {
        el.end.value = '';
        if (el.cleanupHint) el.cleanupHint.textContent='';
        return;
    }

    const dur = state.movieDurationMin; // đã cộng ADS
    if (dur == null) {
        el.end.value = '';
        if (el.cleanupHint) el.cleanupHint.textContent='';
        return;
    }

    const endMin = startMin + dur;
    el.end.value = minutesToHHmm(endMin);

    // gợi ý giờ sớm nhất cho suất kế tiếp (end + 15')
    const nextEarliest = minutesToHHmm(endMin + CLEANUP_MINUTES);
    if (el.cleanupHint) {
        el.cleanupHint.textContent = `Gợi ý: suất kế tiếp (cùng phòng) nên bắt đầu không sớm hơn ${nextEarliest} (đệm ${CLEANUP_MINUTES}p).`;
    }
}

/* ================= Submit ================= */
async function onSubmit() {
    if (state.submitting) return;
    try {
        showError('');

        const periodIdRaw   = el.period.value;
        const periodId      = Number(periodIdRaw);
        const auditoriumRaw = el.auditorium.value;
        const auditoriumId  = Number(auditoriumRaw);
        const language      = el.language.value;
        const price         = Number(el.price.value || 0);
        const date          = el.date.value;
        const start         = el.start.value;

        if (!el.end.value) recalcEnd();
        const end = el.end.value;

        // Validate cơ bản (GIỮ) — nhưng BỎ check end <= start
        if (!periodIdRaw || Number.isNaN(periodId) || periodId <= 0) throw new Error('Vui lòng chọn Screening Period');
        if (!auditoriumRaw || Number.isNaN(auditoriumId) || auditoriumId <= 0) throw new Error('Vui lòng chọn Phòng chiếu');
        if (!date) throw new Error('Vui lòng chọn Ngày');
        if (!start) throw new Error('Vui lòng chọn giờ bắt đầu');
        if (!end) throw new Error('Không xác định được giờ kết thúc (thiếu thời lượng phim). Vui lòng chọn Period hợp lệ.');
        if (!['Vietnamese','English'].includes(language)) throw new Error('Ngôn ngữ chỉ được chọn Tiếng Việt hoặc English');

        // Check trong khoảng period (theo ngày bắt đầu)
        const opt = el.period.selectedOptions?.[0];
        const [pFrom, pTo] = (opt?.getAttribute('data-range') || '').split('..');
        if (pFrom && pTo && (date < pFrom || date > pTo)) {
            throw new Error(`Ngày chiếu phải nằm trong khoảng ${pFrom} → ${pTo}`);
        }

        // Tải slots để check buffer (FE) trước khi gọi BE
        await loadDaySlotsForAuditoriumDay();
        if (violatesBuffer(start, end)) {
            throw new Error(`Khung giờ vi phạm khoảng đệm ${CLEANUP_MINUTES} phút so với suất khác cùng phòng trong ngày.`);
        }

        // Ghép ISO — NẾU end <= start thì coi như qua ngày hôm sau
        const startTime = toISO(date, start);
        const endDateForIso = (end > start) ? date : addDaysYMD(date, 1);
        const endTime = toISO(endDateForIso, end);

        const payload = { periodId, auditoriumId, startTime, endTime, language, price };

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


function setSubmitting(v){
    state.submitting = !!v;
    if (!el.submit) return;
    el.submit.disabled = state.submitting;
    el.submit.innerHTML = state.submitting
        ? `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`
        : `<i class="fa-solid fa-floppy-disk me-2"></i>Lưu`;
}
