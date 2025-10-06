const API_BASE_URL = 'http://localhost:8080';
// SỬA ĐỔI: Sử dụng endpoint ACTIVE cho Client/Staff
const API_URL = API_BASE_URL + '/api/v1/auditoriums/active';
const BRANCH_API_URL = API_BASE_URL + '/api/v1/branches/active'; // Chỉ lấy chi nhánh đang hoạt động
// SỬA ĐỔI: Sử dụng endpoint ACTIVE theo Branch
const API_AUDITORIUM_BY_BRANCH_ACTIVE = API_BASE_URL + '/api/v1/auditoriums/branch';

// Lấy các phần tử DOM cần thiết
const auditoriumsBody = document.getElementById('auditoriums-body');
const loadButton = document.getElementById('load-auditoriums');
const filterBranchSelect = document.getElementById('filterBranchID');
const paginationFooter = document.getElementById('pagination-footer');

// Biến trạng thái Phân trang & Dữ liệu
let isBranchesLoaded = false;
let allAuditoriumsData = [];
const PAGE_SIZE = 10;
let currentPage = 0;

// --- 1. HÀM TẢI DANH SÁCH CHI NHÁNH VÀO DROPDOWN LỌC ---
async function loadBranchesForFilter() {
    if (isBranchesLoaded) return;

    filterBranchSelect.innerHTML = '<option value="" disabled selected>Đang tải...</option>';

    try {
        // Gọi API chỉ lấy Branch ACTIVE
        const response = await fetch(BRANCH_API_URL);
        if (!response.ok) throw new Error('Không thể tải danh sách chi nhánh.');
        const branches = await response.json();

        filterBranchSelect.innerHTML = '<option value="">--- Tất Cả Chi Nhánh ---</option>';

        if (branches.length === 0) {
            filterBranchSelect.innerHTML = '<option value="" disabled>Không có chi nhánh nào hoạt động.</option>';
            return;
        }

        branches.forEach(branch => {
            const branchId = branch.branchId || branch.id;
            const branchName = branch.branchName;
            filterBranchSelect.appendChild(new Option(branchName, branchId));
        });

        isBranchesLoaded = true;

    } catch (error) {
        console.error('Lỗi khi tải chi nhánh:', error);
        filterBranchSelect.innerHTML = '<option value="" disabled selected>Lỗi tải chi nhánh.</option>';
    }
}


// --- 2. HÀM TẢI & HIỂN THỊ AUDITORIUMS (Client-Side Paging & Filtering) ---

/**
 * Tải dữ liệu phòng chiếu.
 * @param {number} page - Trang cần hiển thị (0-based).
 * @param {boolean} loadFromApi - Nếu TRUE, gọi API để tải lại toàn bộ dữ liệu.
 */
async function loadAuditoriums(page = 0, loadFromApi = true) {
    await loadBranchesForFilter(); // Đảm bảo Branch đã được tải
    currentPage = page;

    auditoriumsBody.innerHTML = '<tr><td colspan="5" class="text-center" style="color:var(--muted)">Đang tải danh sách...</td></tr>';
    paginationFooter.innerHTML = '';

    // TẢI DỮ LIỆU GỐC (chỉ khi cần)
    if (loadFromApi) {
        let url;
        const selectedBranchId = filterBranchSelect.value;

        // Logic SỬA ĐỔI: Dùng endpoint ACTIVE khi lọc
        if (selectedBranchId && selectedBranchId !== "") {
            // Sử dụng API /api/v1/auditoriums/branch/{branchId}/active
            url = `${API_AUDITORIUM_BY_BRANCH_ACTIVE}/${selectedBranchId}/active`;
        } else {
            // Sử dụng API /api/v1/auditoriums/active
            url = API_URL;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            allAuditoriumsData = await response.json();
            currentPage = 0; // Luôn reset về trang đầu khi tải lại toàn bộ
        } catch (error) {
            console.error('Lỗi khi tải danh sách phòng chiếu:', error);
            if (typeof Swal !== 'undefined') {
                Swal.fire('Lỗi Kết Nối', 'Không thể tải dữ liệu phòng chiếu từ server.', 'error');
            }
            auditoriumsBody.innerHTML = `<tr><td colspan="5" class="text-center" style="color:var(--red)">Lỗi kết nối: ${error.message}</td></tr>`;
            return;
        }
    }

    // 3. PHÂN TRANG DỮ LIỆU (DÙNG JAVASCRIPT)
    const totalItems = allAuditoriumsData.length;
    const totalPages = Math.ceil(totalItems / PAGE_SIZE);

    const startIndex = currentPage * PAGE_SIZE;
    const endIndex = startIndex + PAGE_SIZE;

    // Lấy các mục của trang hiện tại
    const auditoriumsToDisplay = allAuditoriumsData.slice(startIndex, endIndex);

    // 4. HIỂN THỊ
    auditoriumsBody.innerHTML = '';

    if (auditoriumsToDisplay.length === 0 && totalItems > 0) {
        // Trường hợp người dùng chuyển đến trang cuối cùng bị trống sau khi xóa
        currentPage = totalPages > 0 ? totalPages - 1 : 0;
        loadAuditoriums(currentPage, false);
        return;
    }

    if (auditoriumsToDisplay.length === 0) {
        auditoriumsBody.innerHTML = '<tr><td colspan="5" class="text-center" style="color:var(--muted)">Không tìm thấy phòng chiếu nào đang hoạt động.</td></tr>';
        return;
    }

    auditoriumsToDisplay.forEach(auditorium => {
        const row = auditoriumsBody.insertRow();
        row.insertCell(0).textContent = auditorium.auditoriumID;
        row.insertCell(1).textContent = auditorium.name;
        row.insertCell(2).textContent = auditorium.branchName;
        row.insertCell(3).textContent = auditorium.type;
        row.insertCell(4).textContent = auditorium.capacity;
    });

    // 5. TẠO FOOTER PHÂN TRANG
    renderPagination(totalPages, currentPage);
}


// --- 3. HÀM RENDER PHÂN TRANG ---
function renderPagination(totalPages, currentPage) {
    paginationFooter.innerHTML = '';

    // Nếu chỉ có 1 trang, không cần hiển thị phân trang
    if (totalPages <= 1) return;

    // Nút Previous
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadAuditoriums(${currentPage - 1}, false)">Trước</a>
        </li>
    `;

    // Hiển thị các nút số (tối đa 5 nút)
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);

    if (totalPages > 5) {
        if (currentPage <= 2) endPage = 4;
        else if (currentPage >= totalPages - 3) startPage = totalPages - 5;
    } else {
        endPage = totalPages - 1;
    }

    for (let i = startPage; i <= endPage; i++) {
        // Gọi loadAuditoriums(i, false) để chỉ chuyển trang (không gọi lại API)
        paginationFooter.innerHTML += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadAuditoriums(${i}, false)">${i + 1}</a>
            </li>
        `;
    }

    // Nút Next
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadAuditoriums(${currentPage + 1}, false)">Sau</a>
        </li>
    `;
}


// --- GẮN SỰ KIỆN VÀ KHỞI TẠO ---

// GẮN SỰ KIỆN LỌC/TẢI LẠI: Luôn tải lại dữ liệu từ API (loadFromApi = true)
loadButton.addEventListener('click', () => loadAuditoriums(0, true));
filterBranchSelect.addEventListener('change', () => loadAuditoriums(0, true));

// Tải danh sách khi trang được load lần đầu
document.addEventListener('DOMContentLoaded', () => loadAuditoriums(0, true));