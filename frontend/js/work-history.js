import { workHistoryApi } from './api.js';

const tblBody = document.getElementById('tbl-body');
const btnSearch = document.getElementById('btnSearch');
const btnClear  = document.getElementById('btnClear');

const fAccount   = document.getElementById('f-accountId');
const fAffected  = document.getElementById('f-affectedId');
const fFrom      = document.getElementById('f-from');
const fTo        = document.getElementById('f-to');

const cForm = document.getElementById('createForm');
const eForm = document.getElementById('editForm');
const createModal = new bootstrap.Modal(document.getElementById('createModal'));
const editModal   = new bootstrap.Modal(document.getElementById('editModal'));

let state = { page: 0, size: 20, sort: 'actionTime,DESC', lastQuery: {} };

// ---- utils ----
function toIsoLocal(dtStr){
    if(!dtStr) return null;
    const d = new Date(dtStr);
    return new Date(d.getTime() - d.getTimezoneOffset()*60000).toISOString();
}
function toLocalInputValue(isoStr){
    if(!isoStr) return '';
    const d = new Date(isoStr);
    const off = d.getTimezoneOffset()*60000;
    return new Date(d.getTime()+off).toISOString().slice(0,16);
}
function valOrNull(n){ return n===''?null:Number(n); }
function strOrNull(s){ s = (s||'').trim(); return s===''?null:s; }
function escapeHtml(s){
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function alertErr(e){ console.error(e); alert(e?.message || 'Request failed'); }

// ---- render ----
function renderTable(page){
    if(!page?.items?.length){
        tblBody.innerHTML = `<tr><td colspan="7" class="text-center py-4">No data</td></tr>`;
        return;
    }
    tblBody.innerHTML = page.items.map(r => `
    <tr>
      <td>${r.id}</td>
      <td>#${r.accountId ?? '-'}</td>
      <td>${r.affectedAccountId ?? '-'}</td>
      <td>${escapeHtml(r.action ?? '')}</td>
      <td>${r.actionTime ? new Date(r.actionTime).toLocaleString() : ''}</td>
      <td>${escapeHtml(r.description ?? '')}</td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-light me-1" data-act="edit" data-id="${r.id}">Edit</button>
        <button class="btn btn-sm btn-danger" data-act="del" data-id="${r.id}">Delete</button>
      </td>
    </tr>
  `).join('');
}

async function load(){
    tblBody.innerHTML = `<tr><td colspan="7" class="text-center py-4">Loading…</td></tr>`;
    const q = {
        page: state.page,
        size: state.size,
        sort: state.sort,
        ...state.lastQuery
    };
    try{
        const data = await workHistoryApi.search(q);
        renderTable(data);
    }catch(e){
        alertErr(e);
        tblBody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-4">Failed to load</td></tr>`;
    }
}

// ---- events ----
btnSearch.addEventListener('click', async ()=>{
    state.lastQuery = {
        accountId: fAccount.value || null,
        affectedAccountId: fAffected.value || null,
        from: toIsoLocal(fFrom.value),
        to: toIsoLocal(fTo.value)
    };
    state.page = 0;
    await load();
});

btnClear.addEventListener('click', async ()=>{
    fAccount.value = '';
    fAffected.value = '';
    fFrom.value = '';
    fTo.value = '';
    state.lastQuery = {};
    state.page = 0;
    await load();
});

tblBody.addEventListener('click', async (e)=>{
    const btn = e.target.closest('button[data-act]');
    if(!btn) return;
    const id = btn.dataset.id;

    if(btn.dataset.act === 'del'){
        if(!confirm('Delete this record?')) return;
        try{
            await workHistoryApi.remove(id);
            await load();
        }catch(err){ alertErr(err); }
        return;
    }

    if(btn.dataset.act === 'edit'){
        try{
            const r = await workHistoryApi.get(id);
            document.getElementById('e-id').value = r.id;
            document.getElementById('e-affectedId').value = r.affectedAccountId ?? '';
            document.getElementById('e-action').value = r.action ?? '';
            document.getElementById('e-desc').value = r.description ?? '';
            document.getElementById('e-time').value = toLocalInputValue(r.actionTime);
            editModal.show();
        }catch(err){ alertErr(err); }
    }
});

cForm.addEventListener('submit', async (e)=>{
    e.preventDefault();
    const payload = {
        accountId: Number(document.getElementById('c-accountId').value),
        affectedAccountId: valOrNull(document.getElementById('c-affectedId').value),
        action: strOrNull(document.getElementById('c-action').value),
        description: strOrNull(document.getElementById('c-desc').value),
        actionTime: toIsoLocal(document.getElementById('c-time').value),
    };
    try{
        await workHistoryApi.create(payload);
        createModal.hide();
        cForm.reset();
        await load();
    }catch(err){ alertErr(err); }
});

eForm.addEventListener('submit', async (e)=>{
    e.preventDefault();
    const id = document.getElementById('e-id').value;
    const payload = {
        affectedAccountId: valOrNull(document.getElementById('e-affectedId').value),
        action: strOrNull(document.getElementById('e-action').value),
        description: strOrNull(document.getElementById('e-desc').value),
        actionTime: toIsoLocal(document.getElementById('e-time').value),
    };
    try{
        await workHistoryApi.update(id, payload);
        editModal.hide();
        await load();
    }catch(err){ alertErr(err); }
});

// init
load();
