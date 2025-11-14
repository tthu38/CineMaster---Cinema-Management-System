/// ================= WORK SCHEDULE MATRIX =================
import workScheduleApi from './api/workScheduleApi.js';
import staffApi from './api/staffApi.js';
import { getValidToken, API_BASE_URL } from './api/config.js';


// ===== DOM =====
const weekPicker = document.getElementById('weekPicker');
const branchInp  = document.getElementById('branchId');
const thead      = document.getElementById('thead');
const tbody      = document.getElementById('tbody');
const legend     = document.getElementById('legend');
const rangeText  = document.getElementById('rangeText');
document.getElementById('btnLoad').addEventListener('click', load);


// ================== AI AUTO SCHEDULER (PREVIEW + SAVE) ==================
const btnAuto = document.getElementById("btnAutoSchedule");
const btnConfirmAI = document.getElementById("btnConfirmAI");


let aiRequestData = null;
window._aiPreview = null;


if (btnAuto && btnConfirmAI) {


    // Bấm nút AI -> mở modal xác nhận
    btnAuto.addEventListener("click", () => {
        const branchId = Number(localStorage.getItem("branchId"));
        const weekStr = weekPicker.value;


        if (!branchId || !weekStr) {
            alert("Vui lòng chọn tuần và chi nhánh.");
            return;
        }


        const dates = weekDates(weekStr);
        const weekStart = dates[0];


        aiRequestData = { branchId, weekStart };


        new bootstrap.Modal(document.getElementById("aiConfirmModal")).show();
    });


    // User XÁC NHẬN -> gọi PREVIEW
    btnConfirmAI.addEventListener("click", async () => {


        if (!aiRequestData) return;


        const token = getValidToken();
        const { branchId, weekStart } = aiRequestData;


        const modal = bootstrap.Modal.getInstance(
            document.getElementById("aiConfirmModal")
        );
        modal.hide();
        document.activeElement.blur();


        try {
            btnAuto.disabled = true;
            btnAuto.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Đang tạo AI...`;


            // 🔥 Gọi PREVIEW — đúng API backend
            const res = await fetch(
                `${API_BASE_URL}/ai/preview?branchId=${branchId}&weekStart=${weekStart}`,
                {
                    method: "GET",
                    headers: { Authorization: `Bearer ${token}` }
                }
            );


            if (!res.ok) throw new Error(await res.text());


            const preview = await res.json();


// Backend trả dạng { matrix: { ... } }
            const matrix = preview.matrix || {};


            window._aiPreview = matrix;


// Render đúng dạng matrix map
            renderAIPreview(matrix);




            new bootstrap.Modal(document.getElementById("aiPreviewModal")).show();


        } catch (err) {
            console.error(err);
            alert("❌ Lỗi AI: " + err.message);
        } finally {
            btnAuto.disabled = false;
            btnAuto.innerText = "Tạo lịch tự động (AI)";
        }
    });
}


// ===== LƯU LỊCH AI =====
// ===== LƯU LỊCH AI =====
document.getElementById("btnConfirmSaveAI")?.addEventListener("click", async () => {
    const preview = window._aiPreview;
    if (!preview) return alert("Không có dữ liệu preview!");


    const token = getValidToken();
    const { branchId, weekStart } = aiRequestData;


    // 🔥 Convert preview {id,name} → chỉ còn ID trước khi POST cho backend
    const payload = { matrix: {} };


    for (const [date, shifts] of Object.entries(preview)) {
        payload.matrix[date] = {};


        for (const [shift, staffList] of Object.entries(shifts)) {
            payload.matrix[date][shift] = staffList.map(s => {
                return s.id || s.accountId || s.accountID;
            });
        }
    }


    console.log("AI SAVE PAYLOAD:", payload);


    try {
        const res = await fetch(
            `${API_BASE_URL}/ai/save?branchId=${branchId}&weekStart=${weekStart}`,
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            }
        );


        if (!res.ok) throw new Error(await res.text());


        alert("🎉 Đã lưu lịch AI!");


        bootstrap.Modal.getInstance(
            document.getElementById("aiPreviewModal")
        ).hide();


        document.activeElement.blur();
        await load();


    } catch (err) {
        console.error(err);
        alert("❌ Không thể lưu: " + err.message);
    }
});










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


// ===== ROLE & BRANCH =====
const role = localStorage.getItem("role");
// Hiển thị nút xem yêu cầu cho Manager
if (role === "Manager") {
    document.getElementById("btnViewRequests").style.display = "inline-block";
}


const myBranchId = Number(localStorage.getItem("branchId"));


// ===== AUTO SET BRANCH FOR MANAGER + STAFF =====
if ((role === "Manager" || role === "Staff") && branchInp) {
    branchInp.value = myBranchId;
    branchInp.readOnly = true;
    branchInp.classList.add("bg-light"); // hiển thị kiểu khóa
}




// ===== RESTRICT UI FOR STAFF =====
if (role === "Staff") {
    // Ẩn nút “Chỉnh giờ làm”
    if (btnEditShiftHours) btnEditShiftHours.style.display = "none";


    // Ẩn nút “Lưu” trong modal assign
    if (f.form) {
        const saveBtn = f.form.querySelector('button[type="submit"]');
        if (saveBtn) saveBtn.style.display = "none";
    }


    // Chặn click vào cell để không mở modal assign
    tbody.addEventListener('click', e => {
        e.stopPropagation();
        e.preventDefault();
    }, true);


    // Làm nhạt bảng để user biết là “readonly”
    tbody.classList.add("readonly");
}


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
    if(!branchId){
        alert('Không xác định được chi nhánh làm việc.');
        return;
    }




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


// ===== AI PREVIEW RENDERER =====
function renderAIPreview(matrix) {
    const wrap = document.getElementById("ai-preview-table");
    if (!wrap) return;


    const dates = Object.keys(matrix).sort();
    const shifts = ["MORNING", "AFTERNOON", "NIGHT"];


    let html = `
       <table class="table table-bordered table-dark text-center align-middle">
           <thead>
               <tr>
                   <th>Ca / Ngày</th>
                   ${dates.map(d => `<th>${d}</th>`).join("")}
               </tr>
           </thead>
           <tbody>
   `;


    shifts.forEach(shift => {
        html += `<tr><th>${shift}</th>`;
        dates.forEach(d => {
            const staff = matrix[d]?.[shift] || [];
            html += `<td>`;
            if (staff.length) {
                html += staff.map(s => `
                   <div class="pill d-flex justify-content-between align-items-center">
                       <span>${s.name}</span>
                       <button
                           class="btn btn-sm btn-danger ms-2 ai-remove-btn"
                           data-date="${d}"
                           data-shift="${shift}"
                           data-id="${s.id || s.accountId}">
                           ✖
                       </button>
                   </div>
               `).join("");
            } else {
                html += `<span class="muted">—</span>`;
            }
            html += `</td>`;
        });
        html += `</tr>`;
    });


    html += `</tbody></table>`;
    wrap.innerHTML = html;


    // Gán sự kiện cho nút Reject
    document.querySelectorAll(".ai-remove-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const date = btn.dataset.date;
            const shift = btn.dataset.shift;
            const id = Number(btn.dataset.id);


            // XÓA nhân viên khỏi matrix preview
            window._aiPreview[date][shift] =
                window._aiPreview[date][shift].filter(s =>
                    (s.id || s.accountId) !== id
                );


            // render lại
            renderAIPreview(window._aiPreview);
        });
    });
}






// ===== cell click → modal =====
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
        let selected = new Set((assigned||[]).map(x => x.accountId));


        // Nếu là Staff, chỉ show checkbox của mình
        if(role === "Staff"){
            const meId = Number(localStorage.getItem("accountId"));
            selected = new Set(assigned.some(x=>x.accountId===meId) ? [meId] : []);
            renderStaffCheckboxes(selected, true); // true = Staff mode
        } else {
            renderStaffCheckboxes(selected, false); // Manager/Admin
        }


        assignModal.show();


    }catch(err){
        console.error(err);
        f.err.textContent = 'Không tải được dữ liệu ô.';
    }
});


// ===== renderStaffCheckboxes =====
function renderStaffCheckboxes(selected, staffMode=false){
    let rows = (STAFF_CACHE||[])
        .filter(s => !f.filter.value.trim() || s.fullName.toLowerCase().includes(f.filter.value.trim().toLowerCase()))
        .map(s => {
            if(staffMode && s.id !== Number(localStorage.getItem("accountId"))) return ''; // ẩn nhân viên khác
            return `<label class="d-flex align-items-center gap-2 py-1">
               <input class="form-check-input m-staff" type="checkbox" value="${s.id}" ${selected.has(s.id)?'checked':''}>
               <span>${esc(s.fullName||('#'+s.id))}</span>
           </label>`;
        }).filter(Boolean);


    f.list.innerHTML = rows.join('') || `<div class="muted">Không có nhân viên phù hợp.</div>`;


    // Staff chỉ được thao tác checkbox của mình
    if(staffMode){
        f.list.querySelectorAll('input[type="checkbox"]').forEach(i=>{
            i.disabled = Number(i.value) !== Number(localStorage.getItem("accountId"));
        });
        // hiển thị nút lưu
        f.form.querySelector('button[type="submit"]').style.display = '';
        // ẩn các nút checkAll/uncheckAll
        f.checkAll.style.display = 'none';
        f.uncheckAll.style.display = 'none';
        f.filter.disabled = true;
    } else {
        f.list.querySelectorAll('input[type="checkbox"]').forEach(i=>i.disabled=false);
        f.form.querySelector('button[type="submit"]').style.display = '';
        f.checkAll.style.display = '';
        f.uncheckAll.style.display = '';
        f.filter.disabled = false;
    }
}


// ===== submit form =====
f.form.addEventListener('submit', async (e)=>{
    e.preventDefault();
    f.err.textContent = '';


    let accountIds = [...document.querySelectorAll('.m-staff:checked')].map(i=>Number(i.value));


    if(role === "Staff"){
        // Chỉ gửi ID bản thân
        const meId = Number(localStorage.getItem("accountId"));
        accountIds = accountIds.includes(meId) ? [meId] : [];
    }


    const payload = { branchId: STATE.branchId, date: f.date.value, shiftType: f.shift.value, accountIds };
    try{
        await workScheduleApi.upsertCellMany(payload);
        assignModal.hide();
        document.activeElement.blur();
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
// ===== Weekly Shift Request (staff choose schedule) =====
const DAYS = [
    { key: "mon", label: "Thứ 2" },
    { key: "tue", label: "Thứ 3" },
    { key: "wed", label: "Thứ 4" },
    { key: "thu", label: "Thứ 5" },
    { key: "fri", label: "Thứ 6" },
    { key: "sat", label: "Thứ 7" },
    { key: "sun", label: "Chủ nhật" },
];


function buildShiftRequestTable() {
    const tbody = document.getElementById("sr-table");
    if (!tbody) return;
    tbody.innerHTML = DAYS.map(d => `
       <tr>
           <td>${d.label}</td>
           <td><input type="checkbox" name="${d.key}_MORNING"></td>
           <td><input type="checkbox" name="${d.key}_AFTERNOON"></td>
           <td><input type="checkbox" name="${d.key}_NIGHT"></td>
       </tr>
   `).join('');
}


// Khi mở modal → build bảng
const shiftRequestModal = document.getElementById("shiftRequestModal");
if (shiftRequestModal) {
    shiftRequestModal.addEventListener("show.bs.modal", buildShiftRequestTable);
}
// ===== Submit Request =====
const shiftRequestForm = document.getElementById("shiftRequestForm");


if (shiftRequestForm) {
    shiftRequestForm.addEventListener("submit", async (e) => {
        e.preventDefault();


        const weekStr = document.getElementById("weekStart").value;
        const note = document.getElementById("sr-note").value || "";
        const errBox = document.getElementById("sr-error");


        if (!weekStr) {
            errBox.textContent = "Vui lòng chọn tuần.";
            return;
        }


        // Convert tuần → 7 ngày yyyy-MM-dd
        const dates = weekDates(weekStr);


        // Build danh sách ca làm
        const list = [];


        DAYS.forEach((d, idx) => {
            const date = dates[idx];


            ["MORNING", "AFTERNOON", "NIGHT"].forEach(shift => {
                const el = document.querySelector(`input[name="${d.key}_${shift}"]`);
                if (el && el.checked) {
                    list.push({ date, shiftType: shift });
                }
            });
        });


        const payload = {
            branchId: myBranchId,
            weekStart: dates[0],
            note,
            shifts: list
        };


        console.log("Shift request payload:", payload);


        try {
            await workScheduleApi.submitShiftRequest(payload);
            bootstrap.Modal.getInstance(shiftRequestModal).hide();
            alert("Gửi đăng ký thành công!");
        } catch (err) {
            console.error(err);
            errBox.textContent = "Không gửi được đăng ký!";
        }
    });
}


// Manager xem danh sách yêu cầu ca làm
document.getElementById("btnViewRequests")?.addEventListener("click", async () => {
    const branchId = Number(localStorage.getItem("branchId"));
    const token = getValidToken();


    const res = await fetch(`${API_BASE_URL}/shift-requests/branch/${branchId}`, {
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
        }
    });


    const raw = await res.json();
    const list = raw.data || raw;


    renderManagerRequests(list);


    new bootstrap.Modal(document.getElementById("managerRequestsModal")).show();
});


function renderManagerRequests(list) {
    const tbody = document.getElementById("manager-requests-body");


    if (!list.length) {
        tbody.innerHTML = `<tr><td colspan="7">Không có yêu cầu nào</td></tr>`;
        return;
    }


    tbody.innerHTML = list.map(r => `
       <tr>
           <td>${r.accountName}</td>
           <td>${r.shiftDate}</td>
           <td>${r.shiftType}</td>
           <td>${r.note || "-"}</td>
           <td>${r.createdAt?.replace("T", " ")}</td>
           <td>
               <span class="badge bg-${r.status === "PENDING" ? "warning" : r.status === "APPROVED" ? "success" : "danger"}">
                   ${r.status}
               </span>
           </td>
           <td>
               ${r.status === "PENDING"
        ? `<button class="btn btn-success btn-sm" onclick="approve(${r.requestID})">Duyệt</button>
                      <button class="btn btn-danger btn-sm" onclick="reject(${r.requestID})">Hủy</button>`
        : "-"
    }
           </td>
       </tr>
   `).join("");
}


// Action duyệt / từ chối
window.approve = id => updateStatus(id, "APPROVED");
window.reject  = id => updateStatus(id, "REJECTED");


async function updateStatus(id, status) {
    const token = getValidToken();


    await fetch(`${API_BASE_URL}/shift-requests/${id}/status?status=${status}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
        }
    });


    document.getElementById("btnViewRequests").click();
}






weekPicker.addEventListener('change', load);
load();

