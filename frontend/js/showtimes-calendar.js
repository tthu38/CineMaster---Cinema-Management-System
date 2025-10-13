import { showtimeApi } from './api/showtimeApi.js';
import { branchApi } from './api/branchApi.js';

const dayButtons = document.getElementById('dayButtons');
const contentArea = document.getElementById('contentArea');
const branchSelect = document.getElementById('branchSelect');

let data = [];        // [{ date:'YYYY-MM-DD', movies:[{ movieId, movieTitle, posterUrl, slots:[...] }] }]
let activeIndex = 0;  // tab ngày đang chọn (0..6)
let todayIndex = 0;   // tab hôm nay

/* ====================== Helpers ====================== */
function getSlotShowtimeId(s){ return s?.showtimeId ?? s?.showtimeID ?? s?.id ?? s?.showtime?.id ?? null; }
function getSlotAudId(s){ return s?.auditoriumId ?? s?.auditoriumID ?? s?.auditorium?.auditoriumId ?? null; }
function getSlotAudName(s){ return s?.auditoriumName ?? s?.auditorium?.name ?? (getSlotAudId(s) ? '#'+getSlotAudId(s) : '#?'); }

function getMonday(d){
    const day = d.getDay(); // 0..6 (Sun..Sat)
    const diff = (day + 6) % 7; // Mon=0
    const m = new Date(d);
    m.setHours(0,0,0,0);
    m.setDate(m.getDate() - diff);
    return m;
}
function ymd(d){ return d.toISOString().slice(0,10); }
function fmtLabel(d){ return d.toLocaleDateString('vi-VN',{weekday:'short', day:'2-digit', month:'2-digit'}); }
function formatHm(iso){
    const t = new Date(iso);
    return t.toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});
}
function toBookingUrl(id){ return `/booking.html?showtimeId=${encodeURIComponent(id)}`; }

function sameYMD(a, b){
    const ad = new Date(a); ad.setHours(0,0,0,0);
    const bd = new Date(b); bd.setHours(0,0,0,0);
    return ad.getTime() === bd.getTime();
}

function toDateLocal(v){
    if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return new Date(`${v}T00:00:00`);
    return new Date(v);
}

/* ====================== Render Tabs ====================== */
function renderTabs(weekStart){
    const days = Array.from({length:7}, (_,i)=> new Date(weekStart.getTime()+i*86400000));
    const now = new Date(); now.setHours(0,0,0,0);

    dayButtons.innerHTML = days.map((d,i)=>{
        const isPast = d < now;
        const active = i===activeIndex ? 'active' : '';
        const todayStyle = (i===todayIndex)
            ? 'style="border-color:#0aa3ff; box-shadow:0 0 0 2px rgba(34,193,255,.18) inset;"'
            : '';
        return `<button class="btn ${active}" data-idx="${i}" ${isPast ? 'disabled':''} ${todayStyle}>${fmtLabel(d)}</button>`;
    }).join('');
}

/* ====================== Render Day ====================== */
function renderDay(i){
    const day = data[i];
    if(!day){ contentArea.innerHTML=''; return; }

    const now = new Date();
    const isToday = sameYMD(day.date, now);

    const visibleMovies = (day.movies || []).map(m => {
        const slotsRaw = m.slots || [];
        const filtered = isToday
            ? slotsRaw.filter(s => toDateLocal(s.endTime) > now)
            : slotsRaw;
        return { ...m, slots: filtered };
    }).filter(m => (m.slots && m.slots.length));

    if(!visibleMovies.length){
        contentArea.innerHTML = `<div class="empty">Lịch chiếu trống cho ngày ${new Date(day.date).toLocaleDateString('vi-VN')}</div>`;
        return;
    }

    contentArea.innerHTML = visibleMovies.map(m => `
      <div class="movie-card">
        <div class="movie-head">
          <div class="poster">
            <img src="${m.posterUrl || '/uploads/no-poster.png'}" alt="">
          </div>
          <div class="flex-grow-1">
            <div class="movie-title h5 mb-1">${m.movieTitle}</div>
            <div class="text-muted small">Mã phim #${m.movieId}</div>
          </div>
        </div>
        <div class="slots">
          ${m.slots.map(s => {
        const st = toDateLocal(s.startTime);
        const et = toDateLocal(s.endTime);
        const ongoing = isToday && st <= now && et > now;

        const id = getSlotShowtimeId(s);
        const label = `<span>${formatHm(s.startTime)}–${formatHm(s.endTime)}</span>
                           <span>• Phòng ${getSlotAudName(s)}</span>`;

        const main = (ongoing || !id)
            ? `<span class="slot disabled" title="${ongoing ? 'Đang chiếu' : 'Thiếu ID'}">${label}</span>`
            : `<a class="slot" data-id="${String(id)}" href="${toBookingUrl(id)}" title="Đặt vé">${label}</a>`;

        return `
                <div class="slot-wrap">
                  ${main}
                  <button type="button" class="slot-edit" data-id="${id ?? ''}" title="Sửa" ${!id ? 'disabled' : ''}>
                    <i class="fa-solid fa-pen-to-square"></i>
                  </button>
                </div>`;
    }).join('')}
        </div>
      </div>
    `).join('');
}

/* ====================== Events ====================== */
dayButtons.addEventListener('click',(e)=>{
    const btn = e.target.closest('button[data-idx]');
    if(!btn || btn.disabled) return;
    activeIndex = Number(btn.dataset.idx);
    renderTabs(getMonday(new Date()));
    renderDay(activeIndex);
    window.__calendarSelectedYMD = data?.[activeIndex]?.date || null;
});

contentArea.addEventListener('click', (e) => {
    const editBtn = e.target.closest('button.slot-edit');
    if (editBtn) {
        e.preventDefault();
        const id = editBtn.dataset.id;
        if (!id) return;
        window.openShowtimeEdit?.(id);
        return;
    }

    const a = e.target.closest('a.slot[data-id]');
    if (!a) return;
    if (e.altKey || e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const id = a.dataset.id;
        if (id) window.openShowtimeEdit?.(id);
    }
});

branchSelect.addEventListener('change', load);

/* ====================== Load Data ====================== */
async function loadBranches() {
    try {
        // ✅ Dùng API public, tương thích alias
        const branches = await branchApi.getAllActive() ?? [];
        const options = [{ id: '', branchName: 'Tất cả rạp' }, ...branches];
        branchSelect.innerHTML = options
            .map(b => `<option value="${b.id}">${b.branchName}</option>`)
            .join('');
    } catch (err) {
        console.error('Không tải được danh sách rạp:', err);
        branchSelect.innerHTML = `<option value="">(Không tải được danh sách rạp)</option>`;
    }
}

async function load(){
    const now = new Date();
    const monday = getMonday(now);
    const anchor = ymd(now);
    const branchId = branchSelect.value || undefined;

    contentArea.innerHTML = `<div class="text-center py-5 text-info">Đang tải lịch chiếu...</div>`;

    try {
        // ✅ Dùng API /showtimes/week (public)
        data = await showtimeApi.getWeek({ anchor, branchId }) ?? [];
    } catch(err) {
        console.error(err);
        contentArea.innerHTML = `<div class="alert alert-danger">
          Không tải được lịch chiếu: ${err?.message || 'Lỗi kết nối'}
        </div>`;
        return;
    }

    todayIndex = Math.floor((now - monday)/86400000);
    activeIndex = todayIndex;

    renderTabs(monday);
    renderDay(activeIndex);
    window.__calendarSelectedYMD = data?.[activeIndex]?.date || null;
}

/* ====================== Init ====================== */
(async function init(){
    await loadBranches();
    await load();
})();
window.reloadCalendar = load;
