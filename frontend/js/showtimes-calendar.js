import { showtimeApi } from './api/showtimeApi.js';
import { branchApi } from './api/branchApi.js';

/* ====================== GLOBAL VAR ====================== */
const dayButtons = document.getElementById('dayButtons');
const contentArea = document.getElementById('contentArea');
const branchSelect = document.getElementById('branchSelect');

let data = [];
let activeIndex = 0;
let todayIndex = 0;
let currentAnchor = new Date();
let currentOffset = 0;
let currentBranch = null;
let staffHasShiftToday = true;

/* ================== ROLE DETECTION ================== */
const role = localStorage.getItem("role") || null;
const isAdmin = role === "Admin";
const isManager = role === "Manager";
const isCustomer = role === "Customer";
const isStaff = role === "Staff";
const isGuest = !role || role === "null" || role === "undefined";
const isViewer = isCustomer || isStaff || isGuest;

/* ====================== CONSTANTS ====================== */
const STAFF_BOOK_GRACE_MINUTES = 15;

/* ====================== HELPERS ====================== */
function getMonday(d) {
    const day = d.getDay();
    const diff = (day === 0 ? -6 : 1 - day);
    const m = new Date(d);
    m.setHours(0, 0, 0, 0);
    m.setDate(m.getDate() + diff);
    return m;
}

function ymd(d) {
    const local = new Date(d);
    const y = local.getFullYear();
    const m = String(local.getMonth() + 1).padStart(2, '0');
    const day = String(local.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

function sameYMD(a, b) {
    const da = new Date(a), db = new Date(b);
    da.setHours(0, 0, 0, 0);
    db.setHours(0, 0, 0, 0);
    return da.getTime() === db.getTime();
}

function fmtLabel(d) {
    return d.toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' });
}

function formatHm(iso) {
    return new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function toDateLocal(v) {
    return /^\d{4}-\d{2}-\d{2}$/.test(v) ? new Date(`${v}T00:00:00+07:00`) : new Date(v);
}

/* ====================== TABS ====================== */
function renderTabsViewer() {
    const days = Array.from({ length: 7 }, (_, i) => {
        const d = new Date();
        d.setDate(d.getDate() + i);
        return d;
    });

    dayButtons.innerHTML = days.map((d, i) => {
        const active = i === activeIndex ? "active" : "";
        return `
            <button class="btn ${active}" data-idx="${i}">
                ${fmtLabel(d)}
            </button>
        `;
    }).join("");
}

function renderTabsAdmin(weekStart) {
    const days = Array.from({ length: 7 }, (_, i) =>
        new Date(weekStart.getTime() + i * 86400000)
    );

    const now = new Date();
    now.setHours(0, 0, 0, 0);

    dayButtons.innerHTML = days.map((d, i) => {
        const isPast = d < now;
        const disabled = (!isAdmin && !isManager && isPast) ? "disabled" : "";
        const active = i === activeIndex ? "active" : "";

        return `
            <button class="btn ${active}" data-idx="${i}" ${disabled}>
                ${fmtLabel(d)}
            </button>
        `;
    }).join("");
}

function renderTabs(weekStart) {
    if (isViewer) return renderTabsViewer();
    return renderTabsAdmin(weekStart);
}

/* ====================== RENDER DAY ====================== */
// ❗ Giữ nguyên toàn bộ logic renderDay — không thay đổi
function renderDay(i) {
    const day = data[i];
    if (!day) { contentArea.innerHTML = ''; return; }

    const visibleMovies = (day.movies || []).filter(m => {
        if (!m.slots || !m.slots.length) return false;

        const hasVisibleSlot = m.slots.some(s => {
            const st = new Date(s.startTime);
            const et = new Date(s.endTime);
            const now = new Date();
            const isPast = et.getTime() <= now.getTime();
            const isOngoing = st <= now && et > now;

            if ((isGuest || isCustomer) && isPast && !isOngoing) return false;
            return true;
        });

        return hasVisibleSlot;
    });

    if (!visibleMovies.length) {
        contentArea.innerHTML = `
        <div class="empty text-center">
            Hôm nay không có suất chiếu phù hợp
        </div>`;
        return;
    }

    contentArea.innerHTML = visibleMovies.map(m => `
      <div class="movie-card">
        <div class="movie-poster">
          <img src="${m.posterUrl || '/uploads/no-poster.png'}" alt="${m.movieTitle}">
          <h5>${m.movieTitle}</h5>
        </div>

        <div class="movie-showtimes">
          ${m.slots.map(s => {
        const st = new Date(s.startTime);
        const et = new Date(s.endTime);
        const now = new Date();

        const isPast = et <= now;
        const isOngoing = st <= now && et > now;

        if ((isGuest || isCustomer) && isPast && !isOngoing) return "";

        const diffMinSinceStart = (now - st) / 60000;
        const id = s.showtimeId || s.id;
        const startLabel = formatHm(s.startTime);
        const endLabel = formatHm(s.endTime);
        const roomName = s.auditoriumName || '#?';

        const canBook =
            (["Customer", "Staff"].includes(role)) &&
            !isPast &&
            (
                now < st ||
                (isOngoing && isStaff && diffMinSinceStart <= STAFF_BOOK_GRACE_MINUTES)
            );

        const canEdit = (["Admin", "Manager"].includes(role)) && !isPast && !isOngoing;

        const extraClass = isOngoing ? "ongoing" : "";

        if (isGuest) {
            return `
                <a href="#" class="showtime-slot guest-slot ${extraClass}" data-id="${id}">
                    <span class="time">${startLabel} – ${endLabel}</span>
                    <span class="room">Phòng ${roomName}</span>
                </a>`;
        }

        if (canBook) {
            return `
                <a href="seat-diagram.html?showtimeId=${id}" class="showtime-slot ${extraClass}">
                  <span class="time">${startLabel} – ${endLabel}</span>
                  <span class="room">Phòng ${roomName}</span>
                </a>`;
        }

        if (canEdit) {
            return `
                <a href="#" class="showtime-slot editable ${extraClass}" data-id="${id}">
                  <span class="time">${startLabel} – ${endLabel}</span>
                  <span class="room">Phòng ${roomName}</span>
                </a>`;
        }

        return `
            <span class="showtime-slot disabled ${extraClass}">
              <span class="time">${startLabel} – ${endLabel}</span>
              <span class="room">Phòng ${roomName}</span>
            </span>`;
    }).join('')}
        </div>
      </div>
    `).join('');

    contentArea.querySelectorAll('.showtime-slot.editable').forEach(slot => {
        slot.addEventListener('click', e => {
            e.preventDefault();
            const id = slot.dataset.id;
            if (id) window.openShowtimeEdit?.(id);
        });
    });
}

/* ====================== LOAD DATA (CHỈ PHẦN NÀY ĐƯỢC SỬA) ====================== */
async function load(keepSelectedDay = false) {
    const params = new URLSearchParams(window.location.search);
    const urlBranchId = params.get("branchId");
    const urlMovieId = params.get("movieId");

    const branchId = branchSelect.value && branchSelect.value !== "undefined"
        ? branchSelect.value
        : urlBranchId;
    const movieId = urlMovieId && urlMovieId !== "undefined" ? urlMovieId : null;

    currentBranch = branchId;

    const monday = getMonday(new Date(currentAnchor.getTime() + currentOffset * 7 * 86400000));
    const anchor = ymd(monday);

    contentArea.innerHTML = `<div class="text-center py-5 text-info">Đang tải lịch chiếu...</div>`;

    /* ========================== 🎯 CHỈ THAY ĐOẠN NÀY 🎯 ========================== */
    try {
        if (isViewer) {
            // 🟦 VIEWER → GET 7 DAYS NEXT
            data = await showtimeApi.getNext7Days({
                branchId,
                movieId
            }) ?? [];

            todayIndex = 0;
            activeIndex = 0;

        } else {
            // 🟥 ADMIN & MANAGER → GET WEEK
            data = await showtimeApi.getWeek({
                anchor,
                offset: 0,
                branchId,
                movieId
            }) ?? [];

            todayIndex = Math.floor((new Date() - monday) / 86400000);
            if (todayIndex < 0 || todayIndex > 6) todayIndex = 0;
            activeIndex = todayIndex;
        }

        if (!data || data.length === 0) {
            contentArea.innerHTML = `
                <div class="empty text-center py-5">
                    Không có lịch chiếu nào.
                </div>`;
            return;
        }

    } catch (err) {
        console.error("❌ Lỗi tải lịch chiếu:", err);
        contentArea.innerHTML = `<div class="alert alert-danger">Không tải được lịch chiếu</div>`;
        return;
    }
    /* ========================== 🎯 HẾT PHẦN THAY 🎯 ========================== */


    if (keepSelectedDay && window.__calendarSelectedYMD) {
        const idx = data.findIndex(d => sameYMD(d.date, window.__calendarSelectedYMD));
        if (idx >= 0) activeIndex = idx;
    }

    renderTabs(monday);

    /* ===== Staff: kiểm tra có ca làm không ===== */
    if (isStaff) {
        const staffId = localStorage.getItem("accountId");
        const selectedDate = data?.[activeIndex]?.date;

        try {
            const res = await fetch(`/api/v1/work-schedules/has-shift?accountId=${staffId}&date=${selectedDate}`);
            const js = await res.json();
            staffHasShiftToday = js.hasShift === true;
        } catch (_) {
            staffHasShiftToday = false;
        }

        // if (!staffHasShiftToday) {
        //     contentArea.innerHTML = `
        //     <div class="empty text-center">
        //         <strong>Hôm nay bạn không có ca làm</strong><br>
        //         Bạn không thể đặt vé hoặc hỗ trợ suất chiếu.
        //     </div>`;
        //     return;
        // }
    }

    renderDay(activeIndex);
    updateWeekLabel(monday);
    updateCreateButton();

    window.__calendarSelectedYMD = data?.[activeIndex]?.date || null;
}

/* ====================== WEEK LABEL ====================== */
function updateWeekLabel(monday) {
    if (isViewer) {
        document.getElementById("weekLabel").textContent = "Lịch chiếu 7 ngày tiếp theo";
        return;
    }

    const sunday = new Date(monday.getTime() + 6 * 86400000);
    document.getElementById("weekLabel").textContent =
        `Tuần ${monday.toLocaleDateString('vi-VN')} → ${sunday.toLocaleDateString('vi-VN')}`;
}

/* ====================== CREATE BUTTON ====================== */
// (Giữ nguyên không đổi)
function updateCreateButton() {
    const canManage = ["Admin", "Manager"].includes(role);
    if (!canManage) return;

    const selectedDay = data?.[activeIndex]?.date;
    if (!selectedDay) return;

    const btnCreate = document.getElementById("btnOpenCreate");
    const btnBatch = document.getElementById("btnOpenBatch");
    const btnAI = document.getElementById("btnOpenAI");

    const now = new Date();
    const selected = toDateLocal(selectedDay);
    selected.setHours(23, 59, 59, 999);

    const isPast = ymd(selected) < ymd(now);
    const isToday = ymd(selected) === ymd(now);

    const disableAll = (isPast || isToday);

    [btnCreate, btnBatch, btnAI].forEach(btn => {
        if (!btn) return;
        btn.disabled = disableAll;
        btn.style.opacity = disableAll ? 0.5 : 1;
        btn.title = disableAll
            ? (isPast
                ? "Không thể tạo lịch chiếu trong quá khứ"
                : "Không thể tạo lịch chiếu trong ngày hôm nay")
            : "Tạo lịch chiếu mới";
    });
}

/* ====================== WEEK NAVIGATION ====================== */
document.addEventListener("click", async (e) => {
    const id = e.target.id;

    if (isGuest || isCustomer || isStaff) return;

    if (id === "prevWeekBtn") {
        currentOffset -= 1;
        await load();
    } else if (id === "nextWeekBtn") {
        currentOffset += 1;
        await load();
    }
});

/* ====================== BRANCH ====================== */
async function loadBranches() {
    const params = new URLSearchParams(window.location.search);
    const urlBranchId = params.get("branchId");

    try {
        if (isManager) {
            const branchId = localStorage.getItem("branchId");
            const b = await branchApi.getById(branchId);
            branchSelect.innerHTML = `<option value="${b.id}" selected>${b.branchName}</option>`;
            branchSelect.disabled = true;
            return;
        }

        const branches = await branchApi.getAllActive() ?? [];
        const options = [{ id: '', branchName: 'Tất cả rạp' }, ...branches];

        branchSelect.innerHTML = options.map(b => {
            const id = b.id ?? b.branchId;
            const selected = (urlBranchId && id == urlBranchId) ? 'selected' : '';
            return `<option value="${id}" ${selected}>${b.branchName ?? 'Không tên'}</option>`;
        }).join('');

        if (urlBranchId) {
            branchSelect.value = urlBranchId;
            currentBranch = urlBranchId;
            branchSelect.disabled = true;
        }
    } catch (err) {
        console.error("❌ Lỗi tải chi nhánh:", err);
        branchSelect.innerHTML = `<option value="">(Lỗi tải chi nhánh)</option>`;
    }
}

branchSelect.addEventListener('change', async () => {
    localStorage.setItem("currentBranchId", branchSelect.value);
    await load(true);
});

/* ====================== EVENTS ====================== */
dayButtons.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-idx]');
    if (!btn) return;

    activeIndex = Number(btn.dataset.idx);

    renderTabs(getMonday(new Date(currentAnchor.getTime() + currentOffset * 7 * 86400000)));
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

/* ====================== LOGIN POPUP ====================== */
contentArea.addEventListener('click', (e) => {
    const slotLink = e.target.closest('a.slot, a.guest-slot');
    if (!slotLink) return;

    if (isGuest) {
        e.preventDefault();
        const modalEl = document.getElementById('loginPromptModal');
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
});

document.getElementById('btnGoLogin')?.addEventListener('click', () => {
    const modalEl = document.getElementById('loginPromptModal');
    const modal = bootstrap.Modal.getInstance(modalEl);
    modal?.hide();
    window.location.href = "./login.html";
});

/* ====================== INIT ====================== */
(async function init() {
    if (isGuest || isCustomer || isStaff) {
        document.getElementById("btnOpenCreate")?.remove();
        document.getElementById("btnOpenBatch")?.remove();
        document.getElementById("btnOpenAI")?.remove();
    }

    if (isManager) {
        const branchId = localStorage.getItem("branchId");
        branchSelect.innerHTML = `<option value="${branchId}" selected>Chi nhánh của tôi (#${branchId})</option>`;
        branchSelect.disabled = true;
    } else {
        await loadBranches();
    }

    if (isViewer) {
        document.getElementById("prevWeekBtn").style.display = "none";
        document.getElementById("nextWeekBtn").style.display = "none";
    }

    await load();
})();

window.reloadCalendar = (keepSelectedDay = false) => load(keepSelectedDay);
