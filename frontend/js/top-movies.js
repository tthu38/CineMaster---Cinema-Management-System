import { revenueApi } from './api/revenueApi.js';
import { branchApi } from './api/branchApi.js';


document.addEventListener('DOMContentLoaded', init);


async function init() {
    const role = localStorage.getItem('role')?.toLowerCase();
    const branchId = localStorage.getItem('branchId');
    const branchSelect = document.getElementById('branchSelect');
    const btnLoad = document.getElementById('btnLoadTop');


    // 🏢 Load chi nhánh
    try {
        if (role === 'admin') {
            const branches = await branchApi.getAllActive();
            branchSelect.innerHTML =
                `<option value="">Tất cả chi nhánh</option>` +
                branches.map(b => `<option value="${b.branchId}">${b.branchName}</option>`).join('');


            // 🔁 Khi admin đổi chi nhánh → load lại
            branchSelect.addEventListener('change', async () => {
                const selectedBranch = branchSelect.value || null;
                await loadTopMovies(selectedBranch);
            });
        } else {
            branchSelect.innerHTML = `<option value="${branchId}" selected>Chi nhánh của tôi</option>`;
            branchSelect.disabled = true;
        }
    } catch (e) {
        console.error('❌ Lỗi tải chi nhánh:', e);
        branchSelect.innerHTML = `<option value="">Lỗi tải chi nhánh</option>`;
    }


    // 🚀 Load ban đầu
    const initialBranch = role === 'admin' ? (branchSelect.value || null) : branchId;
    await loadTopMovies(initialBranch);


    // 🔁 Nút reload
    btnLoad.addEventListener('click', async () => {
        const selectedBranch = branchSelect.value || null;
        await loadTopMovies(selectedBranch);
    });
}


/* ============================================================
  🎬 Load top 10 phim
============================================================ */
async function loadTopMovies(branchId) {
    const tbody = document.getElementById('topMoviesBody');
    try {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Đang tải...</td></tr>`;


        // ✅ Chắc chắn không truyền null/undefined lung tung
        const cleanBranch = branchId && branchId.trim() !== '' ? branchId : null;


        const data = await revenueApi.getTopMovies(cleanBranch);


        if (!data || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Không có dữ liệu</td></tr>`;
            return;
        }


        tbody.innerHTML = data.map((item, idx) => `
         <tr>
           <td class="rank text-center">${idx + 1}</td>
           <td>${item.movieTitle}</td>
           <td class="text-center fw-bold text-info">${item.ticketsSold.toLocaleString('vi-VN')}</td>
         </tr>
       `).join('');
    } catch (err) {
        console.error('❌ Lỗi tải top 10 phim:', err);
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
    }
}

