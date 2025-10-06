// =========================================================================
// CẤU HÌNH VÀ BIẾN TOÀN CỤC (VIEW)
// =========================================================================

const API_BASE_URL = 'http://localhost:8080/api/v1';

// ✅ ĐÃ SỬA: Dùng endpoint /screening-periods không lỗi 403
const PERIOD_API_URL_BASE = `${API_BASE_URL}/screening-periods`;

const MOVIE_API_URL = `${API_BASE_URL}/auth`;
const BRANCH_API_URL = `${API_BASE_URL}/branches`;

// Lấy các phần tử DOM
const periodsBody = document.getElementById('periods-body');
const filterBranchSelect = document.getElementById('filterBranchID');
const loadPeriodsBtn = document.getElementById('load-periods');

// ✅ THÊM LẠI PHẦN TỬ PHÂN TRANG
const paginationFooter = document.getElementById('pagination-footer');

// Biến trạng thái & Dữ liệu
let allMovies = [];
let allBranches = []; // Sẽ chỉ chứa branches đang Active
let allPeriodsData = []; // Chứa dữ liệu gốc tải về

// ✅ BIẾN PHÂN TRANG
const PAGE_SIZE = 10;
let currentPage = 0;

// Số cột là 6 (ID, Poster, Phim, CN, BĐ, KT)
const TABLE_COLSPAN = 6;


// =========================================================================
// HÀM TẢI DỮ LIỆU KHÓA NGOẠI (MOVIES & BRANCHES)
// =========================================================================

async function loadForeignKeys() {
    if (!filterBranchSelect) {
        console.error("Lỗi khởi tạo: Không tìm thấy DOM element 'filterBranchID'.");
        return;
    }

    try {
        // --- 1. Tải Danh sách Branch ---
        const branchResponse = await fetch(BRANCH_API_URL);
        const branches = await branchResponse.json();

        // ✅ LỌC BRANCHES: Chỉ giữ lại các chi nhánh có isActive = true để hiển thị trong ô lọc
        allBranches = Array.isArray(branches) ? branches.filter(b => b.isActive !== false) : [];

        filterBranchSelect.innerHTML = '<option value="all" selected>Lọc theo Chi nhánh...</option>';

        if (Array.isArray(allBranches)) {
            allBranches.forEach(branch => {
                const branchId = branch.branchId || branch.id;
                if (branchId) {
                    const optionTextFilter = branch.branchName;
                    filterBranchSelect.appendChild(new Option(optionTextFilter, branchId));
                }
            });
        }

        // --- 2. Tải Danh sách Movie ---
        const movieResponse = await fetch(MOVIE_API_URL);
        const movieApiData = await movieResponse.json();
        allMovies = movieApiData.result || [];

    } catch (error) {
        console.error('Lỗi khi tải khóa ngoại:', error);
        Swal.fire('Cảnh báo', 'Không thể tải đầy đủ danh sách Phim hoặc Chi nhánh.', 'warning');
    }
}


// =========================================================================
// HÀM TẢI & HIỂN THỊ DANH SÁCH (READ + PHÂN TRANG + LỌC ACTIVE)
// =========================================================================

/**
 * @param {boolean} loadFromApi - Tải lại dữ liệu từ API.
 * @param {number} page - Trang muốn hiển thị (chỉ dùng khi loadFromApi=false)
 */
async function loadPeriods(loadFromApi = false, page = 0) {
    if (!periodsBody || !filterBranchSelect) {
        console.error("Lỗi tải: Không tìm thấy DOM elements.");
        return;
    }

    // Đặt trang hiện tại
    currentPage = page;

    const branchIdFilter = filterBranchSelect.value;
    periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải dữ liệu...</td></tr>`;

    // 1. Tải Dữ liệu gốc
    if (loadFromApi) {
        let url = PERIOD_API_URL_BASE;

        // Xây dựng URL API theo bộ lọc (gọi /branch/{id} nếu có)
        if (branchIdFilter !== 'all') {
            url = `${PERIOD_API_URL_BASE}/branch/${branchIdFilter}`;
        }

        try {
            const response = await fetch(url);

            if (response.status === 404) {
                allPeriodsData = [];
            } else if (!response.ok) {
                throw new Error('Failed to fetch periods. Status: ' + response.status);
            } else {
                allPeriodsData = await response.json();
            }
            currentPage = 0; // Reset về trang đầu khi tải lại từ API
        } catch (error) {
            console.error('Lỗi tải danh sách:', error);
            periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-danger">Lỗi khi tải dữ liệu: ${error.message}</td></tr>`;
            return;
        }
    }

    // 2. ✅ LỌC DỮ LIỆU ĐANG HOẠT ĐỘNG (PERIODS)
    let periodsToProcess = allPeriodsData.filter(period => {
        // Chỉ giữ lại period nếu isActive không tồn tại hoặc là TRUE
        return period.isActive !== false;
    });

    // 3. Xử lý Phân trang (Client-side)
    const totalItems = periodsToProcess.length;
    const totalPages = Math.ceil(totalItems / PAGE_SIZE);

    const startIndex = currentPage * PAGE_SIZE;
    const endIndex = startIndex + PAGE_SIZE;

    const periodsToDisplay = periodsToProcess.slice(startIndex, endIndex);

    // 4. Hiển thị
    periodsBody.innerHTML = '';

    if (periodsToDisplay.length === 0) {
        periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted">Không tìm thấy khoảng thời gian chiếu nào đang hoạt động.</td></tr>`;
        if(paginationFooter) paginationFooter.innerHTML = '';
        return;
    }

    periodsToDisplay.forEach(period => {

        const movie = allMovies.find(m => String(m.movieID || m.id) === String(period.movieId));
        const movieTitle = movie ? movie.title : `Phim ID: ${period.movieId}`;
        const posterUrl = movie && movie.posterUrl ?
            movie.posterUrl :
            '../images/default-poster.png';

        const row = periodsBody.insertRow();

        row.insertCell(0).textContent = period.id;
        row.insertCell(1).innerHTML = `<img src="${posterUrl}" alt="${movieTitle}" class="movie-poster-img" onerror="this.onerror=null;this.src='../images/default-poster.png';">`;
        row.insertCell(2).textContent = movieTitle;

        // Mapping Branch Name (dùng dữ liệu đã lọc)
        let branchNameDisplay = period.branchName || 'N/A';
        if (!period.branchName && period.branchId) {
            const branch = allBranches.find(b => String(b.branchId || b.id) === String(period.branchId));
            branchNameDisplay = branch ? branch.branchName : `Chi nhánh ID: ${period.branchId}`;
        }
        row.insertCell(3).textContent = branchNameDisplay;

        row.insertCell(4).textContent = new Date(period.startDate).toLocaleDateString('vi-VN');
        row.insertCell(5).textContent = new Date(period.endDate).toLocaleDateString('vi-VN');
    });

    // 5. Tạo footer phân trang
    renderPagination(totalPages, currentPage);
}


// =========================================================================
// HÀM XỬ LÝ PHÂN TRANG (Client-Side)
// =========================================================================

function renderPagination(totalPages, currentPage) {
    if (!paginationFooter) return;

    paginationFooter.innerHTML = '';

    // Nút Previous
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPeriods(false, ${currentPage - 1})">Trước</a>
        </li>
    `;

    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);

    if (totalPages > 5) {
        if (currentPage <= 2) endPage = 4;
        else if (currentPage >= totalPages - 3) startPage = totalPages - 5;
    } else {
        endPage = totalPages - 1;
    }

    for (let i = startPage; i <= endPage; i++) {
        // loadPeriods(false, i) chỉ chuyển trang, không gọi lại API
        paginationFooter.innerHTML += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadPeriods(false, ${i})">${i + 1}</a>
            </li>
        `;
    }

    // Nút Next
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPeriods(false, ${currentPage + 1})">Sau</a>
        </li>
    `;
}


// =========================================================================
// GẮN SỰ KIỆN & KHỞI TẠO
// =========================================================================

// Gắn sự kiện cho bộ lọc: Tải lại dữ liệu (true) khi thay đổi bộ lọc
if (filterBranchSelect) {
    filterBranchSelect.addEventListener('change', () => loadPeriods(true));
}

// Gắn sự kiện cho nút Tải lại
if (loadPeriodsBtn) {
    loadPeriodsBtn.addEventListener('click', () => loadPeriods(true));
}


document.addEventListener('DOMContentLoaded', async () => {
    // Tải các khóa ngoại trước
    await loadForeignKeys();
    // Tải danh sách khoảng chiếu lần đầu (loadFromApi = true)
    loadPeriods(true);
});