// ================= WORK SCHEDULE MATRIX =================
import workScheduleApi from './api/workScheduleApi.js';
import staffApi from './api/staffApi.js';

// ===== DOM =====
const weekPicker = document.getElementById('weekPicker');
const branchInp  = document.getElementById('branchId');
const thead      = document.getElementById('thead');
const tbody      = document.getElementById('tbody');
const legend     = document.getElementById('legend');
const rangeText  = document.getElementById('rangeText');
document.getElementById('btnLoad').addEventListener('click', load);

const btnEditShiftHours = document.getElementById('btnEditShiftHours');
const shiftHoursModalEl = document.getElementById('shiftHoursModal');
const shiftHoursForm  = document.getElementById('shiftHoursForm');
const shiftHoursErr   = document.getElementById('shiftHoursError');
const HINP = {
    mFrom: document.getElementById('h-morning-from'),
    mTo:   document.getElementById('h-morning-to'),
    aFrom: document.getElementById('h-afternoon-from'),
    aTo:   document.getElementById('h-afternoon-to'),
    nFrom: document.getElementById('h-night-from') || document.getElementById('h-evening-from'),
    nTo:   document.getElementById('h-night-to')   || document.getElementById('h-evening-to'),
};
const assignModal = new bootstrap.Modal(document.getElementById('assignModal'));
const shiftHoursModal = shiftHoursModalEl ? new bootstrap.Modal(shiftHoursModalEl) : null;

const f = {
    date: document.getElementById('m-date'),
    shift: document.getElementById('m-shift'),
    hint: document.getElementById('m-hint'),
    err: document.getElementById('m-error'),
    filter: document.getElementById('m-filter'),
    checkAll: document.getElementById('m-checkAll'),
    uncheckAll: document.getElementById('m-uncheckAll'),
    list: document.getElementById('m-staffList'),
    form: document.getElementById('assignForm'),
};

// ===== constants =====
const SHIFTS = ['MORNING', 'AFTERNOON', 'NIGHT'];
const PALETTE = [
    css('--p1') || '#22C1FF',
    css('--p2') || '#0AA3FF',
    '#3DDC97','#F39C12','#9B59B6','#E74C3C','#16A085','#3498DB','#E67E22',
    css('--p7') || '#1B2B4A'
];
const colorForId = id => PALETTE[Math.abs(Number(id) || 0) % PALETTE.length];

// ===== state =====
let STATE = { branchId: null, dates: [], matrix: {} };
let STAFF_CACHE = [];

// ===== utils =====
function css(varName){ return getComputedStyle(document.documentElement).getPropertyValue(varName).trim(); }
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function weekDates(weekStr){
    const [Y,W] = weekStr.split('-W').map(Number);
    const jan4 = new Date(Date.UTC(Y,0,4));
    const dow  = jan4.getUTCDay() || 7;
    const thu1 = new Date(jan4); thu1.setUTCDate(jan4.getUTCDate()-dow+1+3);
    const thu  = new Date(thu1); thu.setUTCDate(thu1.getUTCDate()+(W-1)*7);
    const mon  = new Date(thu);  mon.setUTCDate(thu.getUTCDate()-3);
    return [...Array(7)].map((_,i)=>{ const d=new Date(mon); d.setUTCDate(mon.getUTCDate()+i); return d.toISOString().slice(0,10); });
}
const wd = d => ['CN','T2','T3','T4','T5','T6','T7'][new Date(d).getDay()];
const dayLabel = d => `${wd(d)} ${d}`;
function buildEmpty(dates){ const m={}; dates.forEach(dt => m[dt]={MORNING:[],AFTERNOON:[],NIGHT:[]}); return m; }
function applyMatrix(m, dataMap){
    for(const [date, byShift] of Object.entries(dataMap||{})){
        if(!m[date]) continue;
        for(const [shift, arr] of Object.entries(byShift||{})){
            m[date][shift] = (arr||[]).map(x => ({
                id: x.accountId ?? x.id ?? x.accountID,
                name: x.accountName ?? x.fullName ?? ''
            }));
        }
    }
}

// ===== shift hour utils =====
const SHIFT_TYPES = SHIFTS;
function hoursKey(branchId){ return `cm.display.shiftHours.branch.${branchId||'null'}`; }
function defaultHours(){
    return {
        MORNING:   { from:'08:00', to:'13:00' },
        AFTERNOON: { from:'13:00', to:'18:00' },
        NIGHT:     { from:'18:00', to:'23:00' },
    };
}
function loadDisplayHours(branchId){
    try{
        const raw = localStorage.getItem(hoursKey(branchId));
        return raw ? JSON.parse(raw) : defaultHours();
    }catch{ return defaultHours(); }
}
function saveDisplayHours(branchId, hours){
    localStorage.setItem(hoursKey(branchId), JSON.stringify(hours));
}
function shiftLabel(shift, hours){
    const h = hours?.[shift] || defaultHours()[shift];
    return `${h.from}–${h.to}`;
}
const timeRe = /^([01]\d|2[0-3]):[0-5]\d$/;
const validTime = t => timeRe.test(t);

// ===== render =====
function renderHeader(){
    thead.innerHTML = `
  <tr>
    <th class="sticky-col" style="min-width:160px">Ca làm \\ Ngày</th>
    ${STATE.dates.map(d => `<th style="white-space:pre">${esc(dayLabel(d))}</th>`).join('')}
  </tr>`;
}

function renderBody(){
    const dispHours = loadDisplayHours(STATE.branchId);
    tbody.innerHTML = SHIFTS.map(shift => {
        const cols = STATE.dates.map(date => {
            const list = STATE.matrix?.[date]?.[shift] || [];
            const inner = list.length
                ? `<div class="cell-stack">${list.map(p => `
          <span class="pill" style="background:${colorForId(p.id)}" title="#${p.id}">
            ${esc(p.name || ('#'+p.id))}
          </span>`).join('')}</div>`
                : `<span class="muted">—</span>`;
            return `<td class="schedule-cell" data-date="${date}" data-shift="${shift}">${inner}</td>`;
        }).join('');
        return `<tr>
      <th class="sticky-col text-start">
        <div class="fw-bold">${esc(shiftLabel(shift, dispHours))}</div>
        <div class="small muted">${esc(shift)}</div>
      </th>${cols}
    </tr>`;
    }).join('');
}

function renderLegend(){
    const seen = new Map();
    for(const d of STATE.dates) for(const s of SHIFTS)
        (STATE.matrix[d][s]||[]).forEach(p => { if(!seen.has(p.id)) seen.set(p.id,p.name); });
    legend.innerHTML = seen.size
        ? [...seen].map(([id,name]) => `<span class="chip" style="background:${colorForId(id)}" title="#${id}">${esc(name||('#'+id))}</span>`).join('')
        : `<span class="muted">Chưa có lịch trong tuần này.</span>`;
}

// ===== load data =====
async function load(){
    const wk = weekPicker.value;
    if(!wk) return;
    const branchId = branchInp.value ? Number(branchInp.value) : null;
    if(!branchId){ alert('Vui lòng nhập Branch ID.'); return; }

    STATE.branchId = branchId;
    STATE.dates = weekDates(wk);
    STATE.matrix = buildEmpty(STATE.dates);
    renderHeader();
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4">Loading…</td></tr>`;

    const H = loadDisplayHours(branchId);
    rangeText.textContent =
        `Branch #${branchId} • Tuần: ${STATE.dates[0]} → ${STATE.dates[6]} • `
        + `Ca sáng ${H.MORNING.from}–${H.MORNING.to}, `
        + `Ca chiều ${H.AFTERNOON.from}–${H.AFTERNOON.to}, `
        + `Ca tối ${H.NIGHT.from}–${H.NIGHT.to}`;

    try{
        const dataMap = await workScheduleApi.getMatrix({ from: STATE.dates[0], to: STATE.dates[6], branchId });
        applyMatrix(STATE.matrix, dataMap);
        renderBody();
        renderLegend();

        try {
            STAFF_CACHE = await staffApi.getByBranch(branchId);
        } catch (err) {
            console.warn("Không tải được danh sách staff:", err);
            STAFF_CACHE = [];
        }

    } catch(err) {
        console.error(err);
        alert('Không tải được lịch làm việc. Kiểm tra API backend.');
    }
}

// ===== cell click → modal =====
tbody.addEventListener('click', async (e)=>{
    const td = e.target.closest('.schedule-cell');
    if(!td) return;
    const date = td.dataset.date;
    const shift = td.dataset.shift;
    f.date.value = date;
    f.shift.value = shift;
    f.err.textContent = '';
    f.hint.textContent = `${date} • ${shift} • Branch #${STATE.branchId}`;

    try{
        const assigned = await workScheduleApi.getCell({ branchId: STATE.branchId, date, shiftType: shift });
        const selected = new Set((assigned||[]).map(x => x.accountId));
        renderStaffCheckboxes(selected);
        assignModal.show();
    }catch(err){
        console.error(err);
        f.err.textContent = 'Không tải được dữ liệu ô.';
    }
});

function renderStaffCheckboxes(selected){
    const q = f.filter.value.trim().toLowerCase();
    const rows = (STAFF_CACHE||[])
        .filter(s => !q || (s.fullName||'').toLowerCase().includes(q))
        .map(s => `<label class="d-flex align-items-center gap-2 py-1">
      <input class="form-check-input m-staff" type="checkbox" value="${s.id}" ${selected.has(s.id)?'checked':''}>
      <span>${esc(s.fullName||('#'+s.id))}</span>
    </label>`);
    f.list.innerHTML = rows.join('') || `<div class="muted">Không có nhân viên phù hợp.</div>`;
}
f.filter.addEventListener('input', ()=>{
    const selected = new Set([...document.querySelectorAll('.m-staff:checked')].map(i=>Number(i.value)));
    renderStaffCheckboxes(selected);
});
f.checkAll.addEventListener('click', ()=>{ document.querySelectorAll('.m-staff').forEach(i=>i.checked=true); });
f.uncheckAll.addEventListener('click', ()=>{ document.querySelectorAll('.m-staff').forEach(i=>i.checked=false); });

f.form.addEventListener('submit', async (e)=>{
    e.preventDefault();
    f.err.textContent = '';
    const accountIds = [...document.querySelectorAll('.m-staff:checked')].map(i => Number(i.value));
    const payload = { branchId: STATE.branchId, date: f.date.value, shiftType: f.shift.value, accountIds };
    try{
        await workScheduleApi.upsertCellMany(payload);
        assignModal.hide();
        document.activeElement.blur(); // FIX aria-hidden warning
        await load();
    }catch(err){
        console.error(err);
        f.err.textContent = err?.message || 'Assign failed';
    }
});

// ===== Edit shift hours =====
if (btnEditShiftHours) {
    btnEditShiftHours.addEventListener('click', ()=>{
        if(!STATE.branchId){ alert('Vui lòng nhập Branch ID trước.'); return; }
        const hours = loadDisplayHours(STATE.branchId);
        if (shiftHoursModal && HINP.mFrom && HINP.mTo && HINP.aFrom && HINP.aTo && HINP.nFrom && HINP.nTo) {
            HINP.mFrom.value = hours.MORNING.from; HINP.mTo.value = hours.MORNING.to;
            HINP.aFrom.value = hours.AFTERNOON.from; HINP.aTo.value = hours.AFTERNOON.to;
            HINP.nFrom.value = hours.NIGHT.from; HINP.nTo.value = hours.NIGHT.to;
            if (shiftHoursErr) shiftHoursErr.textContent = '';
            shiftHoursModal.show();
        }
    });
}

if (shiftHoursForm) {
    shiftHoursForm.addEventListener('submit', (e)=>{
        e.preventDefault();
        if(!STATE.branchId){ alert('Vui lòng nhập Branch ID trước.'); return; }

        const newHours = {
            MORNING:   { from: HINP.mFrom.value, to: HINP.mTo.value },
            AFTERNOON: { from: HINP.aFrom.value, to: HINP.aTo.value },
            NIGHT:     { from: HINP.nFrom.value, to: HINP.nTo.value },
        };
        for(const s of SHIFT_TYPES){
            const {from,to} = newHours[s];
            if(!validTime(from) || !validTime(to) || to <= from){
                if (shiftHoursErr) shiftHoursErr.textContent = `Giờ không hợp lệ ở ${s}: cần HH:MM và End > Start`;
                return;
            }
        }
        saveDisplayHours(STATE.branchId, newHours);
        if (shiftHoursModal) shiftHoursModal.hide();
        document.activeElement.blur(); // fix aria-hidden warning
        renderBody();

        const H = newHours;
        rangeText.textContent =
            `Branch #${STATE.branchId} • ${STATE.dates[0]} → ${STATE.dates[6]} • `
            + `Ca sáng ${H.MORNING.from}–${H.MORNING.to}, `
            + `Ca chiều ${H.AFTERNOON.from}–${H.AFTERNOON.to}, `
            + `Ca tối ${H.NIGHT.from}–${H.NIGHT.to}`;
    });
}

// ===== init tuần hiện tại =====
(function initWeek(){
    const now=new Date(), y=now.getFullYear();
    const jan4=new Date(Date.UTC(y,0,4)), dow=jan4.getUTCDay()||7;
    const thu1=new Date(jan4); thu1.setUTCDate(jan4.getUTCDate()-dow+1+3);
    const w=Math.floor((now-thu1)/(7*24*3600*1000))+1;
    weekPicker.value = `${y}-W${String(Math.max(1,w)).padStart(2,'0')}`;
})();
weekPicker.addEventListener('change', load);
load();
