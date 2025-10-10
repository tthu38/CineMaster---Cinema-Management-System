// /js/showtime-edit.js
import { showtimeApi, screeningPeriodApi, auditoriumApi } from './api.js';

const ADS_MINUTES = 5;
const CLEANUP_MINUTES = 15;

let modal, el = {}, state = {
    editingId: null,
    movieDurationMin: null,
    daySlots: [],
    submitting: false
};

export async function initShowtimeEdit({ htmlPath } = {}) {
    if (htmlPath) {
        const html = await fetch(htmlPath).then(r => r.text());
        const wrap = document.createElement('div');
        wrap.innerHTML = html;
        document.body.appendChild(wrap);
    }
    modal = new bootstrap.Modal(document.getElementById('showtimeEditModal'));

    el.alert       = document.getElementById('steAlert');
    el.branch      = document.getElementById('steBranch');
    el.period      = document.getElementById('stePeriod');
    el.periodHint  = document.getElementById('stePeriodHint');
    el.auditorium  = document.getElementById('steAuditorium');
    el.language    = document.getElementById('steLanguage');
    el.date        = document.getElementById('steDate');
    el.start       = document.getElementById('steStart');
    el.end         = document.getElementById('steEnd');
    el.price       = document.getElementById('stePrice');
    el.cleanupHint = document.getElementById('steCleanupHint');
    el.submit      = document.getElementById('steSubmit');
    el.deleteBtn   = document.getElementById('steDelete');

    el.end.readOnly = true;

    el.period.addEventListener('change', onPeriodChange);
    el.start.addEventListener('input', recalcEnd);
    el.auditorium.addEventListener('change', async () => {
        await loadDaySlotsForAuditoriumDay();
        recalcEnd();
    });
    el.date.addEventListener('change', async () => {
        await reloadPeriodsAndAuditoriums();
        await loadDaySlotsForAuditoriumDay();
        recalcEnd();
    });

    el.submit.addEventListener('click', (e) => { e?.preventDefault?.(); onSubmitUpdate(); });
    el.deleteBtn.addEventListener('click', onDeleteClick);
}

/* ============= Public API ============= */
export async function openShowtimeEdit(showtimeId) {
    if (!showtimeId && showtimeId !== 0) { // chống null/undefined/''
        alert('Không xác định được ID lịch chiếu để sửa.');
        return;
    }

    showError('');
    if (el.cleanupHint) el.cleanupHint.textContent = '';
    state.editingId = showtimeId;
    state.movieDurationMin = null;
    state.daySlots = [];

    // load showtime detail
    const s = await showtimeApi.get(showtimeId);

    const branchId     = getBranchIdFromShowtime(s);
    const auditoriumId = getAuditoriumIdFromShowtime(s);
    const periodId     = getPeriodIdFromShowtime(s);
    const language     = s?.language ?? 'Vietnamese';
    const price        = s?.price ?? 120000;

    const startIso = s?.startTime;
    const endIso   = s?.endTime;
    const { ymd: startYMD, hm: startHM } = splitISO(startIso);
    const { hm: endHM } = splitISO(endIso);

    await loadBranches();
    el.branch.value = branchId ? String(branchId) : '';

    await reloadPeriodsAndAuditoriums({ branchId, onDate: startYMD });

    if (periodId) el.period.value = String(periodId);
    onPeriodChange();
    if (auditoriumId) el.auditorium.value = String(auditoriumId);

    el.language.value = ['Vietnamese','English'].includes(language) ? language : 'Vietnamese';
    el.price.value    = String(price);
    el.date.value     = startYMD;
    el.start.value    = startHM;
    el.end.value      = endHM;

    await loadDaySlotsForAuditoriumDay();
    recalcEnd();

    modal.show();
}


/* ============= Data loading & helpers ============= */
async function reloadPeriodsAndAuditoriums({ branchId = null, onDate = null } = {}) {
    const bId = (branchId != null ? branchId : Number(el.branch.value || 0)) || null;
    const day = onDate || el.date.value || todayYMD();

    // reset
    el.period.innerHTML = `<option value="">— Chọn period —</option>`;
    el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
    el.periodHint.textContent = '';
    state.movieDurationMin = null;

    const [periods, auds] = await Promise.all([
        screeningPeriodApi.active({ branchId: bId, onDate: day }),
        auditoriumApi.listByBranch(bId),
    ]);

    const periodOptions = (periods || []).map(p => {
        const pid   = getPeriodIdFromPeriod(p);
        const from  = getStartDate(p);
        const to    = getEndDate(p);
        const title = getMovieTitle(p);
        const dur   = getDuration(p);
        return `<option value="${pid ?? ''}"
                    data-range="${from}..${to}"
                    ${dur != null ? `data-duration="${dur}"` : ''}>
              ${title} (${from} → ${to})
            </option>`;
    }).join('');
    el.period.innerHTML = `<option value="">— Chọn period —</option>${periodOptions}`;

    const audOptions = (auds || []).map(a => {
        const aid = getAuditoriumId(a);
        return `<option value="${aid}">
              ${a.name} • ${a.type} • ${a.capacity} ghế
            </option>`;
    }).join('');
    el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>${audOptions}`;
}

async function loadBranches() {
    // demo list — thay bằng API branch thật nếu có
    const branches = [
        { id: 1, name: 'Branch Hanoi' },
        { id: 2, name: 'Branch HCM' },
        { id: 3, name: 'Branch Da Nang' },
    ];
    el.branch.innerHTML = branches.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
}

async function loadDaySlotsForAuditoriumDay() {
    const auditoriumRaw = el.auditorium.value;
    const date = el.date.value;
    if (!auditoriumRaw || !date) { state.daySlots = []; return; }

    const from = `${date}T00:00:00`;
    const d = new Date(date + 'T00:00:00');
    const next = new Date(d.getTime() + 86400000);
    const to = `${next.getFullYear()}-${String(next.getMonth()+1).padStart(2,'0')}-${String(next.getDate()).padStart(2,'0')}T00:00:00`;

    const resp = await showtimeApi.search({
        auditoriumId: Number(auditoriumRaw),
        from, to, page: 0, size: 200, sort: 'startTime,asc'
    });
    // loại chính nó khỏi danh sách
    const id = state.editingId;
    state.daySlots = (resp?.content || []).filter(x => getShowtimeId(x) !== id);
}
function rangeWithBuffer(startStr, endStr) {
    const s = hhmmToMinutes(startStr);
    const e = hhmmToMinutes(endStr);
    if (s == null || e == null) return null;
    // nếu end <= start => qua ngày hôm sau -> +1440
    const s0 = s;
    const e0 = e + (e <= s ? 1440 : 0);
    // nới 2 đầu theo CLEANUP_MINUTES
    return [s0 - CLEANUP_MINUTES, e0 + CLEANUP_MINUTES];
}
function overlaps(aStart, aEnd, bStart, bEnd) {
    return aStart < bEnd && aEnd > bStart;
}
function violatesBuffer(startStr, endStr) {
    if (!state.daySlots?.length) return false;

    const A = rangeWithBuffer(startStr, endStr);
    if (!A) return false;
    let [a1, a2] = A;

    for (const s of state.daySlots) {
        // lấy HH:mm từ ISO
        const sStartHM = splitISO(s.startTime).hm;
        const sEndHM   = splitISO(s.endTime).hm;

        const B = rangeWithBuffer(sStartHM, sEndHM);
        if (!B) continue;
        let [b1, b2] = B;

        // so trùng 2 chiều (kể cả khi 1 bên “kéo” sang ngày +1440)
        if (overlaps(a1, a2, b1, b2)) return true;
        if (overlaps(a1 + 1440, a2 + 1440, b1, b2)) return true;
        if (overlaps(a1, a2, b1 + 1440, b2 + 1440)) return true;
    }
    return false;
}

/* ============= UI reactions ============= */
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

    const durAttr = opt?.getAttribute('data-duration');
    const dur = durAttr ? Number(durAttr) : null;
    state.movieDurationMin = (dur != null && !Number.isNaN(dur)) ? (dur + ADS_MINUTES) : null;

    if (dur != null && !Number.isNaN(dur)) {
        el.periodHint.textContent += `${el.periodHint.textContent ? ' • ' : ''}Thời lượng: ${dur} phút (+${ADS_MINUTES}p QC)`;
    }
    recalcEnd();
}

function recalcEnd() {
    const startStr = el.start.value;
    const startMin = hhmmToMinutes(startStr);
    if (startMin == null) {
        el.end.value = '';
        if (el.cleanupHint) el.cleanupHint.textContent = '';
        return;
    }

    const dur = state.movieDurationMin;
    if (dur == null) {
        el.end.value = '';
        if (el.cleanupHint) el.cleanupHint.textContent = '';
        return;
    }

    const endMin = startMin + dur;
    el.end.value = minutesToHHmm(endMin);

    const nextEarliest = minutesToHHmm(endMin + CLEANUP_MINUTES);
    if (el.cleanupHint) {
        el.cleanupHint.textContent = `Gợi ý: suất kế tiếp (cùng phòng) nên bắt đầu không sớm hơn ${nextEarliest} (đệm ${CLEANUP_MINUTES}p).`;
    }
}

/* ============= Submit update / delete ============= */
async function onSubmitUpdate() {
    if (state.submitting) return;
    try {
        showError('');
        const id            = state.editingId;
        const periodId      = Number(el.period.value);
        const auditoriumId  = Number(el.auditorium.value);
        const language      = el.language.value;
        const price         = Number(el.price.value || 0);
        const date          = el.date.value;
        const start         = el.start.value;

        if (!el.end.value) recalcEnd();
        const end           = el.end.value;

        // validate (không check end <= start, vì qua đêm sẽ cộng ngày)
        if (!id) throw new Error('Thiếu showtimeId');
        if (!periodId) throw new Error('Vui lòng chọn Screening Period');
        if (!auditoriumId) throw new Error('Vui lòng chọn Phòng chiếu');
        if (!date) throw new Error('Vui lòng chọn Ngày');
        if (!start) throw new Error('Vui lòng chọn giờ bắt đầu');
        if (!end) throw new Error('Không xác định được giờ kết thúc (thiếu thời lượng phim).');
        if (!['Vietnamese','English'].includes(language)) throw new Error('Ngôn ngữ chỉ được chọn Tiếng Việt hoặc English');

        // trong khoảng period
        const opt = el.period.selectedOptions?.[0];
        const [pFrom, pTo] = (opt?.getAttribute('data-range') || '').split('..');
        if (pFrom && pTo && (date < pFrom || date > pTo)) {
            throw new Error(`Ngày chiếu phải nằm trong khoảng ${pFrom} → ${pTo}`);
        }

        // FE buffer
        await loadDaySlotsForAuditoriumDay();
        if (violatesBuffer(start, end)) {
            throw new Error(`Khung giờ vi phạm khoảng đệm ${CLEANUP_MINUTES} phút so với suất khác cùng phòng trong ngày.`);
        }

        // build times — nếu end <= start thì end là ngày hôm sau
        const startTime = toISO(date, start);
        const endDateForIso = (end > start) ? date : addDaysYMD(date, 1);
        const endTime   = toISO(endDateForIso, end);

        const payload = { periodId, auditoriumId, startTime, endTime, language, price };

        setSubmitting(true, 'update');
        await showtimeApi.update(id, payload);

        window.dispatchEvent(new Event('showtime:updated'));
        modal.hide();
    } catch (e) {
        showError(e?.message || 'Cập nhật lịch chiếu thất bại');
    } finally {
        setSubmitting(false);
    }
}

async function onDeleteClick() {
    if (!state.editingId) return;
    const ok = confirm('Xóa lịch chiếu này? Hành động không thể hoàn tác.');
    if (!ok) return;

    try {
        setSubmitting(true, 'delete');
        await showtimeApi.remove(state.editingId);
        window.dispatchEvent(new Event('showtime:deleted'));
        modal.hide();
    } catch (e) {
        showError(e?.message || 'Xóa lịch chiếu thất bại');
    } finally {
        setSubmitting(false);
    }
}

/* ============= Tiny utils ============= */
function showError(msg) {
    if (!msg) { el.alert.classList.add('d-none'); el.alert.textContent=''; return; }
    el.alert.textContent = msg;
    el.alert.classList.remove('d-none');
}
function todayYMD() {
    const d = new Date();
    const m = d.getMonth() + 1, day = d.getDate();
    return `${d.getFullYear()}-${String(m).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}
function addDaysYMD(ymd, days){
    const [y,m,d] = ymd.split('-').map(Number);
    const dt = new Date(y, m-1, d);
    dt.setDate(dt.getDate() + days);
    const yy = dt.getFullYear();
    const mm = String(dt.getMonth()+1).padStart(2,'0');
    const dd = String(dt.getDate()).padStart(2,'0');
    return `${yy}-${mm}-${dd}`;
}
function toISO(dateStr, timeStr) { return `${dateStr}T${timeStr}:00`; }

function hhmmToMinutes(str){
    if(!str) return null;
    const [h,m] = str.split(':').map(Number);
    if (Number.isNaN(h) || Number.isNaN(m)) return null;
    return h*60 + m;
}
function minutesToHHmm(mins){
    if (mins == null) return '';
    mins = ((mins % 1440) + 1440) % 1440;
    const h = Math.floor(mins / 60), m = mins % 60;
    return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
}

// getters linh hoạt
function getShowtimeId(s) {
    return s?.showtimeID ?? s?.showtimeId ?? s?.id ?? null;
}
function getBranchIdFromShowtime(s){
    return s?.auditorium?.branch?.id ?? s?.period?.branch?.id ?? s?.branchId ?? null;
}
function getAuditoriumIdFromShowtime(s){
    return s?.auditorium?.auditoriumID ?? s?.auditorium?.auditoriumId ?? s?.auditoriumId ?? null;
}
function getPeriodIdFromShowtime(s){
    return s?.period?.id ?? s?.period?.periodId ?? s?.periodId ?? null;
}

function getPeriodIdFromPeriod(p){ return p?.periodId ?? p?.id ?? p?.screeningPeriodId ?? p?.screeningPeriodID ?? null; }
function getMovieTitle(p){ return p?.movieTitle ?? p?.movie?.title ?? p?.title ?? 'Unknown'; }
function getStartDate(p){ return p?.startDate ?? p?.start ?? p?.from ?? ''; }
function getEndDate(p){ return p?.endDate ?? p?.end ?? p?.to ?? ''; }
function getDuration(p){ return p?.duration ?? p?.movieDuration ?? p?.movie?.duration ?? null; }
function getAuditoriumId(a){ return a?.auditoriumId ?? a?.auditoriumID ?? a?.id ?? null; }

function splitISO(iso){
    // "YYYY-MM-DDTHH:mm[:ss]"
    if (!iso) return { ymd:'', hm:'' };
    const [d,t] = iso.split('T');
    const hm = (t || '').slice(0,5);
    return { ymd: d, hm };
}

function setSubmitting(v, mode){
    state.submitting = !!v;
    if (el.submit) {
        el.submit.disabled = state.submitting;
        el.submit.innerHTML = state.submitting && mode==='update'
            ? `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Đang lưu...`
            : `<i class="fa-solid fa-floppy-disk me-2"></i>Lưu thay đổi`;
    }
    if (el.deleteBtn) el.deleteBtn.disabled = state.submitting;
}
