// ================= IMPORTS =================
import { showtimeApi } from './api/showtimeApi.js';
import { screeningPeriodApi } from './api/screeningPeriodApi.js';
import { auditoriumApi } from './api/auditoriumApi.js';
import { branchApi } from './api/branchApi.js';

// ================= CONSTANTS =================
const ADS_MINUTES = 5;      // phút quảng cáo đầu phim
const CLEANUP_MINUTES = 15; // đệm dọn rạp

// ================= STATE =================
let modal, el = {}, state = {
    editingId: null,
    movieDurationMin: null,
    daySlots: [],
    submitting: false
};

// ================= INIT =================
export async function initShowtimeEdit({ htmlPath } = {}) {
    if (htmlPath) {
        const html = await fetch(htmlPath).then(r => r.text());
        const wrap = document.createElement('div');
        wrap.innerHTML = html;
        document.body.appendChild(wrap);
    }

    modal = new bootstrap.Modal(document.getElementById('showtimeEditModal'));

    el = {
        alert:        document.getElementById('steAlert'),
        branch:       document.getElementById('steBranch'),
        period:       document.getElementById('stePeriod'),
        periodHint:   document.getElementById('stePeriodHint'),
        auditorium:   document.getElementById('steAuditorium'),
        language:     document.getElementById('steLanguage'),
        date:         document.getElementById('steDate'),
        start:        document.getElementById('steStart'),
        end:          document.getElementById('steEnd'),
        price:        document.getElementById('stePrice'),
        cleanupHint:  document.getElementById('steCleanupHint'),
        submit:       document.getElementById('steSubmit'),
        deleteBtn:    document.getElementById('steDelete')
    };
    el.end.readOnly = true;

    // ===== EVENT BINDINGS =====
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

    el.submit.addEventListener('click', e => { e.preventDefault(); onSubmitUpdate(); });
    el.deleteBtn.addEventListener('click', onDeleteClick);
}

// ================= OPEN MODAL =================
export async function openShowtimeEdit(showtimeId) {
    if (!showtimeId && showtimeId !== 0) {
        alert('Không xác định được ID lịch chiếu để sửa.');
        return;
    }

    showError('');
    state.editingId = showtimeId;
    state.movieDurationMin = null;
    state.daySlots = [];

    const s = await showtimeApi.getById(showtimeId);
    const branchId     = getBranchIdFromShowtime(s);
    const auditoriumId = getAuditoriumIdFromShowtime(s);
    const periodId     = getPeriodIdFromShowtime(s);
    const language     = s?.language ?? 'Vietnamese';
    const price        = s?.price ?? 120000;

    const startIso = s?.startTime;
    const endIso   = s?.endTime;
    const { ymd: startYMD, hm: startHM } = splitISO(startIso);
    const { hm: endHM } = splitISO(endIso);

    console.log('🎬 Showtime data:', { branchId, auditoriumId, periodId, startYMD });

    // 🔹 1. Load chi nhánh trước
    await loadBranches();

    // 🔒 Nếu là Manager → khóa dropdown chi nhánh
    const role = localStorage.getItem("role");
    const managerBranch = localStorage.getItem("branchId");
    if (role === "Manager" && managerBranch) {
        el.branch.value = String(managerBranch);
        el.branch.disabled = true;
    }


    // 🔹 2. Set giá trị branch sau khi dropdown đã render
    if (branchId) {
        el.branch.value = String(branchId);
        console.log('✅ Branch set:', el.branch.value);
    } else {
        console.warn('⚠️ Không tìm thấy branchId trong showtime');
    }

    // 🔹 3. Load periods và auditorium theo branch + ngày
    await reloadPeriodsAndAuditoriums({ branchId: Number(el.branch.value), onDate: startYMD });

    // 🔹 4. Set lại giá trị period và auditorium
    if (periodId) el.period.value = String(periodId);
    onPeriodChange();
    if (auditoriumId) el.auditorium.value = String(auditoriumId);

    // 🔹 5. Gán các field khác
    el.language.value = ['Vietnamese', 'English'].includes(language) ? language : 'Vietnamese';
    el.price.value    = String(price);
    el.date.value     = startYMD;
    el.start.value    = startHM;
    el.end.value      = endHM;

    await loadDaySlotsForAuditoriumDay();
    recalcEnd();

    modal.show();
}

// ================= LOAD DATA =================
async function reloadPeriodsAndAuditoriums({ branchId = null, onDate = null } = {}) {
    const bId = (branchId ?? Number(el.branch.value)) || null;
    const day = onDate || el.date.value || todayYMD();

    console.log('🔄 Reloading periods & auditoriums', { bId, day });

    el.period.innerHTML = `<option value="">— Chọn period —</option>`;
    el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>`;
    el.periodHint.textContent = '';
    state.movieDurationMin = null;

    if (!bId) {
        console.warn('⚠️ Không có branchId, bỏ qua reload.');
        return;
    }

    try {
        const [periods, auds] = await Promise.all([
            screeningPeriodApi.active({ branchId: bId, onDate: day }),
            auditoriumApi.getByBranch(bId)
        ]);

        console.log('🟢 Periods:', periods);
        console.log('🟢 Auditoriums:', auds);

        // build dropdown period
        const periodOptions = (periods || []).map(p => {
            const pid = getPeriodIdFromPeriod(p);
            const from = getStartDate(p);
            const to = getEndDate(p);
            const title = getMovieTitle(p);
            const dur = getDuration(p);
            return `<option value="${pid ?? ''}" data-range="${from}..${to}" ${dur ? `data-duration="${dur}"` : ''}>
                        ${title} (${from} → ${to})
                    </option>`;
        }).join('');
        el.period.innerHTML = `<option value="">— Chọn period —</option>${periodOptions}`;

        // build dropdown auditorium
        const audOptions = (auds || []).map(a => {
            const aid = getAuditoriumId(a);
            const name = a?.name ?? a?.auditoriumName ?? 'Phòng';
            const type = a?.type ?? a?.auditoriumType ?? '';
            const cap  = a?.capacity ?? a?.seatCount ?? '';
            return `<option value="${aid}">${name} • ${type} • ${cap} ghế</option>`;
        }).join('');
        el.auditorium.innerHTML = `<option value="">— Chọn phòng —</option>${audOptions}`;

    } catch (err) {
        console.error('❌ Lỗi load periods/auditoriums:', err);
        showError('Không tải được dữ liệu kỳ chiếu hoặc phòng chiếu.');
    }
}

/* ================= LOAD BRANCHES (phân quyền Manager) ================= */
async function loadBranches() {
    try {
        const role = localStorage.getItem("role");
        const branchId = localStorage.getItem("branchId");

        // 🔒 Manager chỉ được xem đúng chi nhánh của mình
        if (role === "Manager" && branchId) {
            const branch = await branchApi.getById(branchId);
            if (branch) {
                el.branch.innerHTML = `
                    <option value="${branch.id ?? branch.branchId}" selected>
                        ${branch.name ?? branch.branchName ?? "Chi nhánh của tôi"}
                    </option>`;
                el.branch.disabled = true;
            } else {
                el.branch.innerHTML = `<option value="">(Không tải được chi nhánh của bạn)</option>`;
                el.branch.disabled = true;
            }
            return;
        }

        // 🟢 Admin xem được tất cả chi nhánh
        const branches = await branchApi.getAllActive() ?? [];
        el.branch.innerHTML = branches
            .map(b => `<option value="${b.id ?? b.branchId}">
                ${b.name ?? b.branchName}
            </option>`)
            .join('');
        el.branch.disabled = false;

    } catch (e) {
        console.error('❌ Lỗi load branches:', e);
        showError('Không tải được danh sách chi nhánh.');
        el.branch.innerHTML = `<option value="">(Không tải được rạp)</option>`;
    }
}


// ================= BUFFER & DURATION =================
async function loadDaySlotsForAuditoriumDay() {
    const auditoriumRaw = el.auditorium.value;
    const date = el.date.value;
    if (!auditoriumRaw || !date) { state.daySlots = []; return; }

    const from = `${date}T00:00:00`;
    const nextDay = addDaysYMD(date, 1);
    const to = `${nextDay}T00:00:00`;

    const resp = await showtimeApi.search({
        auditoriumId: Number(auditoriumRaw),
        from, to, page: 0, size: 200, sort: 'startTime,asc'
    });
    const id = state.editingId;
    state.daySlots = (resp?.content || []).filter(x => getShowtimeId(x) !== id);
}

function violatesBuffer(startStr, endStr) {
    if (!state.daySlots?.length) return false;
    const toMinutes = t => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };

    const startMin = toMinutes(startStr);
    const endMin = toMinutes(endStr);
    for (const s of state.daySlots) {
        const sStart = toMinutes(s.startTime.split('T')[1].slice(0, 5));
        const sEnd = toMinutes(s.endTime.split('T')[1].slice(0, 5));
        if (sStart < endMin + CLEANUP_MINUTES && sEnd > startMin - CLEANUP_MINUTES)
            return true;
    }
    return false;
}

// ================= PERIOD CHANGE =================
function onPeriodChange() {
    const opt = el.period.selectedOptions?.[0];
    const range = opt?.getAttribute('data-range') || '';

    const [from, to] = range.split('..');
    if (from && to) {
        el.date.min = from;
        el.date.max = to;
        if (el.date.value < from || el.date.value > to) el.date.value = from;
    } else {
        el.date.removeAttribute('min');
        el.date.removeAttribute('max');
    }

    const durAttr = opt?.getAttribute('data-duration');
    const dur = durAttr ? Number(durAttr) : null;
    state.movieDurationMin = dur ? dur + ADS_MINUTES : null;

    if (dur) {
        el.periodHint.textContent += `${el.periodHint.textContent ? ' • ' : ''}Thời lượng: ${dur} phút (+${ADS_MINUTES}p QC)`;
    }

    recalcEnd();
}

// ================= RECALC END =================
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
    if (el.cleanupHint) {
        el.cleanupHint.textContent = `Gợi ý: suất kế tiếp (cùng phòng) nên bắt đầu không sớm hơn ${nextEarliest} (đệm ${CLEANUP_MINUTES}p).`;
    }
}

// ================= SUBMIT & DELETE =================
async function onSubmitUpdate() {
    if (state.submitting) return;
    try {
        showError('');
        const id = state.editingId;
        const periodId = Number(el.period.value);
        const auditoriumId = Number(el.auditorium.value);
        const language = el.language.value;
        const price = Number(el.price.value || 0);
        const date = el.date.value;
        const start = el.start.value;

        if (!el.end.value) recalcEnd();
        const end = el.end.value;

        if (!id) throw new Error('Thiếu showtimeId');
        if (!periodId) throw new Error('Vui lòng chọn Screening Period');
        if (!auditoriumId) throw new Error('Vui lòng chọn Phòng chiếu');
        if (!date) throw new Error('Vui lòng chọn Ngày');
        if (!start) throw new Error('Vui lòng chọn giờ bắt đầu');
        if (!end) throw new Error('Không xác định được giờ kết thúc (thiếu thời lượng phim).');
        if (!['Vietnamese', 'English'].includes(language))
            throw new Error('Ngôn ngữ chỉ được chọn Tiếng Việt hoặc English');

        const opt = el.period.selectedOptions?.[0];
        const [pFrom, pTo] = (opt?.getAttribute('data-range') || '').split('..');
        if (pFrom && pTo && (date < pFrom || date > pTo)) {
            throw new Error(`Ngày chiếu phải nằm trong khoảng ${pFrom} → ${pTo}`);
        }

        await loadDaySlotsForAuditoriumDay();
        if (violatesBuffer(start, end)) {
            throw new Error(`Khung giờ vi phạm khoảng đệm ${CLEANUP_MINUTES} phút so với suất khác cùng phòng.`);
        }

        const startTime = toISO(date, start);
        const endDateForIso = end > start ? date : addDaysYMD(date, 1);
        const endTime = toISO(endDateForIso, end);
        const payload = { periodId, auditoriumId, startTime, endTime, language, price };

        setSubmitting(true, 'update');
        await showtimeApi.update(id, payload);

        window.dispatchEvent(new Event('showtime:updated'));
        document.activeElement?.blur(); // tránh warning aria-hidden
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
        document.activeElement?.blur();
        modal.hide();
    } catch (e) {
        showError(e?.message || 'Xóa lịch chiếu thất bại');
    } finally {
        setSubmitting(false);
    }
}

// ================= HELPERS =================
function showError(msg) {
    if (!msg) { el.alert.classList.add('d-none'); el.alert.textContent = ''; return; }
    el.alert.textContent = msg;
    el.alert.classList.remove('d-none');
}
function todayYMD() { const d = new Date(); return d.toISOString().slice(0, 10); }
function addDaysYMD(ymd, days) { const [y,m,d]=ymd.split('-').map(Number);const dt=new Date(y,m-1,d);dt.setDate(dt.getDate()+days);return dt.toISOString().slice(0,10);}
function toISO(dateStr, timeStr) { return `${dateStr}T${timeStr}:00`; }
function hhmmToMinutes(str) { if (!str) return null; const [h,m]=str.split(':').map(Number); return h*60+m; }
function minutesToHHmm(mins) { if (mins==null) return ''; mins=((mins%1440)+1440)%1440; const h=Math.floor(mins/60),m=mins%60; return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`; }

// getters
function getShowtimeId(s){return s?.showtimeID??s?.showtimeId??s?.id??null;}
function getBranchIdFromShowtime(s){return s?.auditorium?.branch?.id??s?.period?.branch?.id??s?.branchId??null;}
function getAuditoriumIdFromShowtime(s){return s?.auditorium?.auditoriumID??s?.auditorium?.auditoriumId??s?.auditoriumId??null;}
function getPeriodIdFromShowtime(s){return s?.period?.id??s?.period?.periodId??s?.periodId??null;}
function getPeriodIdFromPeriod(p){return p?.periodId??p?.id??null;}
function getMovieTitle(p){return p?.movieTitle??p?.movie?.title??p?.title??'Unknown';}
function getStartDate(p){return p?.startDate??'';}
function getEndDate(p){return p?.endDate??'';}
function getDuration(p){return p?.duration??p?.movie?.duration??null;}
function getAuditoriumId(a){
    return a?.auditoriumId ?? a?.auditoriumID ?? a?.id ?? null;
}

function splitISO(iso){if(!iso)return{ymd:'',hm:''};const[d,t]=iso.split('T');return{ymd:d,hm:(t||'').slice(0,5)};}
function setSubmitting(v,mode){state.submitting=!!v;if(el.submit){el.submit.disabled=v;el.submit.innerHTML=v&&mode==='update'?`<span class="spinner-border spinner-border-sm me-2" role="status"></span>Đang lưu...`:`<i class="fa-solid fa-floppy-disk me-2"></i>Lưu thay đổi`;}if(el.deleteBtn)el.deleteBtn.disabled=v;}
