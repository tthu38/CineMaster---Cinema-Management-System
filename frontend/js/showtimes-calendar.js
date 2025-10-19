import { showtimeApi } from './api/showtimeApi.js';
import { branchApi } from './api/branchApi.js';

/* ====================== GLOBAL VAR ====================== */
const dayButtons = document.getElementById('dayButtons');
const contentArea = document.getElementById('contentArea');
const branchSelect = document.getElementById('branchSelect');

let data = [];
let activeIndex = 0;
let todayIndex = 0;
let currentAnchor = new Date();  // ngày gốc
let currentOffset = 0;           // số tuần lệch
let currentBranch = null;

const role = localStorage.getItem("role");

/* ====================== HELPERS ====================== */
function getMonday(d){
    const day = d.getDay(); // 0=Sun
    const diff = (day + 6) % 7; // Mon=0
    const m = new Date(d);
    m.setHours(0,0,0,0);
    m.setDate(m.getDate() - diff);
    return m;
}
function ymd(d){ return d.toISOString().slice(0,10); }
function sameYMD(a,b){ const da=new Date(a), db=new Date(b); da.setHours(0,0,0,0); db.setHours(0,0,0,0); return da.getTime()===db.getTime(); }
function fmtLabel(d){ return d.toLocaleDateString('vi-VN',{weekday:'short',day:'2-digit',month:'2-digit'}); }
function formatHm(iso){ return new Date(iso).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'}); }
function toDateLocal(v){ return /^\d{4}-\d{2}-\d{2}$/.test(v)?new Date(`${v}T00:00:00`):new Date(v); }

/* ====================== RENDER TABS ====================== */
function renderTabs(weekStart){
    const days = Array.from({length:7}, (_,i)=> new Date(weekStart.getTime()+i*86400000));
    const now = new Date(); now.setHours(0,0,0,0);

    dayButtons.innerHTML = days.map((d,i)=>{
        const isPast = d < now;
        const disabled = (!["Admin","Manager"].includes(role) && isPast) ? 'disabled' : '';
        const active = i===activeIndex ? 'active' : '';
        const todayStyle = (i===todayIndex)
            ? 'style="border-color:#0aa3ff; box-shadow:0 0 0 2px rgba(34,193,255,.18) inset;"'
            : '';
        return `<button class="btn ${active}" data-idx="${i}" ${disabled} ${todayStyle}>${fmtLabel(d)}</button>`;
    }).join('');
}

/* ====================== RENDER DAY ====================== */
function renderDay(i){
    const day = data[i];
    if(!day){ contentArea.innerHTML=''; return; }

    const now = new Date();
    const visibleMovies = (day.movies || []).filter(m => (m.slots && m.slots.length));

    if(!visibleMovies.length){
        contentArea.innerHTML = `<div class="empty">Lịch chiếu trống cho ngày ${new Date(day.date).toLocaleDateString('vi-VN')}</div>`;
        return;
    }

    contentArea.innerHTML = visibleMovies.map(m => `
      <div class="movie-card">
        <div class="movie-head">
          <div class="poster"><img src="${m.posterUrl || '/uploads/no-poster.png'}" alt=""></div>
          <div class="flex-grow-1"><div class="movie-title h5 mb-1">${m.movieTitle}</div></div>
        </div>
        <div class="slots">
          ${m.slots.map(s => {
        // Chuẩn hoá UTC -> local
        const st = new Date(s.startTime);
        const et = new Date(s.endTime);
        const now = new Date();
        const isPast = et.getTime() <= now.getTime(); // suất đã kết thúc
        const isOngoing = st <= now && et > now;      // suất đang chiếu

        const id = s.showtimeId || s.id;
        const label = `<span>${formatHm(s.startTime)}–${formatHm(s.endTime)}</span>
                       <span>• Phòng ${s.auditoriumName || '#?'} </span>`;

        const canBook = (["Staff","Customer"].includes(role)) && !isPast && !isOngoing;
        const canEdit = (["Admin","Manager"].includes(role)) && !isPast && !isOngoing;

        // ✅ GẮN LIÊN KẾT GHẾ (seat-diagram.html)
        const main = canBook
            ? `<a class="slot" href="seat-diagram.html?showtimeId=${id}" title="Đặt vé">${label}</a>`
            : `<span class="slot disabled">${label}</span>`;

        return `
          <div class="slot-wrap">
            ${main}
            ${canEdit
            ? `<button type="button" class="slot-edit" data-id="${id}" title="Chỉnh sửa">
                     <i class="fa-solid fa-pen-to-square"></i>
                   </button>`
            : ""}
          </div>`;
    }).join('')}
        </div>
      </div>
    `).join('');
}

/* ====================== LOAD DATA ====================== */
async function load(keepSelectedDay = false){
    const branchId = branchSelect.value && branchSelect.value !== "undefined" ? branchSelect.value : null;
    currentBranch = branchId;
    const anchor = ymd(currentAnchor);
    contentArea.innerHTML = `<div class="text-center py-5 text-info">Đang tải lịch chiếu...</div>`;

    try {
        data = await showtimeApi.getWeek({ anchor, offset: currentOffset, branchId }) ?? [];
    } catch (err) {
        console.error(err);
        contentArea.innerHTML = `<div class="alert alert-danger">Không tải được lịch chiếu</div>`;
        return;
    }

    const monday = getMonday(new Date(currentAnchor.getTime() + currentOffset * 7 * 86400000));
    todayIndex = Math.floor((new Date() - monday)/86400000);
    activeIndex = todayIndex;
    if (keepSelectedDay && window.__calendarSelectedYMD) {
        const idx = data.findIndex(d => sameYMD(d.date, window.__calendarSelectedYMD));
        if (idx >= 0) activeIndex = idx;
    }

    renderTabs(monday);
    renderDay(activeIndex);
    updateWeekLabel(monday);
    updateCreateButton();
    window.__calendarSelectedYMD = data?.[activeIndex]?.date || null;
}

/* ====================== UPDATE WEEK LABEL ====================== */
function updateWeekLabel(monday){
    const sunday = new Date(monday.getTime() + 6 * 86400000);
    document.getElementById("weekLabel").textContent =
        `Tuần ${monday.toLocaleDateString('vi-VN')} → ${sunday.toLocaleDateString('vi-VN')}`;
}

/* ====================== UPDATE CREATE BUTTON ====================== */
function updateCreateButton() {
    const btn = document.getElementById("btnOpenCreate");
    if (!btn || !["Admin","Manager"].includes(role)) return;
    const selectedDay = data?.[activeIndex]?.date;
    if (!selectedDay) return;

    const now = new Date();
    const day = new Date(selectedDay);
    day.setHours(23,59,59,999);

    if (day < now) {
        btn.disabled = true;
        btn.style.opacity = 0.5;
        btn.title = "Không thể tạo lịch chiếu trong quá khứ";
    } else {
        btn.disabled = false;
        btn.style.opacity = 1;
        btn.title = "Tạo lịch chiếu mới";
    }
}

/* ====================== WEEK NAVIGATION ====================== */
document.addEventListener("click", async (e) => {
    const id = e.target.id;
    const isAdminOrManager = (role === "Admin" || role === "Manager");
    if (id === "prevWeekBtn") {
        if (!isAdminOrManager) return;
        currentOffset -= 1;
        await load();
    }
    else if (id === "nextWeekBtn") {
        if (!isAdminOrManager) return;
        currentOffset += 1;
        await load();
    }
});

/* ====================== BRANCH ====================== */
async function loadBranches() {
    try {
        if (role === "Manager") {
            const branchId = localStorage.getItem("branchId");
            const b = await branchApi.getById(branchId);
            branchSelect.innerHTML = `<option value="${b.id}" selected>${b.branchName}</option>`;
            branchSelect.disabled = true;
            return;
        }
        const branches = await branchApi.getAllActive() ?? [];
        const options = [{ id: '', branchName: 'Tất cả rạp' }, ...branches];
        branchSelect.innerHTML = options.map(b =>
            `<option value="${b.id ?? b.branchId}">${b.branchName ?? 'Không tên'}</option>`
        ).join('');
    } catch (err) {
        branchSelect.innerHTML = `<option value="">(Lỗi tải chi nhánh)</option>`;
    }
}
branchSelect.addEventListener('change', () => load(true));

/* ====================== CLICK EVENTS ====================== */
dayButtons.addEventListener('click',(e)=>{
    const btn = e.target.closest('button[data-idx]');
    if(!btn) return;
    activeIndex = Number(btn.dataset.idx);
    renderTabs(getMonday(new Date(currentAnchor.getTime() + currentOffset*7*86400000)));
    renderDay(activeIndex);
    updateCreateButton();
    window.__calendarSelectedYMD = data?.[activeIndex]?.date || null;
});
contentArea.addEventListener('click', (e) => {
    const editBtn = e.target.closest('button.slot-edit');
    if (editBtn) {
        const id = editBtn.dataset.id;
        if (id) window.openShowtimeEdit?.(id);
    }
});

/* ====================== INIT ====================== */
(async function init() {
    if (role === "Manager") {
        const branchId = localStorage.getItem("branchId");
        branchSelect.innerHTML = `<option value="${branchId}" selected>Chi nhánh của tôi (#${branchId})</option>`;
        branchSelect.disabled = true;
    } else if (role === "Admin") {
        await loadBranches();
    }
    if (role !== "Admin" && role !== "Manager") {
        document.getElementById("prevWeekBtn").style.display = "none";
        document.getElementById("nextWeekBtn").style.display = "none";
    }
    await load();
})();
window.reloadCalendar = (keepSelectedDay = false) => load(keepSelectedDay);
