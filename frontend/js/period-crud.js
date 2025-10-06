// =========================================================================
// CẤU HÌNH VÀ BIẾN TOÀN CỤC (CRUD)
// =========================================================================

const API_BASE_URL = 'http://localhost:8080/api/v1';
const PERIOD_API_URL = `${API_BASE_URL}/screening-periods`;
const MOVIE_API_URL = `${API_BASE_URL}/auth`; // Theo MovieController (Endpoint Phim)
const BRANCH_API_URL = `${API_BASE_URL}/branches`; // Theo BranchController

// Lấy các phần tử DOM (Form)
const periodForm = document.getElementById('period-form');
const periodsBody = document.getElementById('periods-body');
const formTitle = document.getElementById('form-title');
const submitBtn = document.getElementById('submit-btn');
const cancelBtn = document.getElementById('cancel-btn');
const periodID = document.getElementById('periodID');

const movieSelect = document.getElementById('movieID');
const branchCheckboxContainer = document.getElementById('branchCheckboxContainer');
const filterBranchSelect = document.getElementById('filterBranchID');
const startDateInput = document.getElementById('startDate');
const endDateInput = document.getElementById('endDate');

const branchHint = document.getElementById('branch-hint');

// Lấy các phần tử DOM (Poster Preview & Pagination)
const posterPreviewContainer = document.getElementById('movie-poster-preview');
const posterPreviewImg = document.getElementById('poster-img-preview');
const paginationFooter = document.getElementById('pagination-footer');

// Biến trạng thái Phân trang & Dữ liệu
let allMovies = [];
let allBranches = [];
let allPeriodsData = []; // LƯU TRỮ TOÀN BỘ DỮ LIỆU KHOẢNG CHIẾU
const PAGE_SIZE = 10;
let currentPage = 0;
// Số cột hiển thị trong bảng
const TABLE_COLSPAN = 8;


// =========================================================================
// HÀM TẢI DỮ LIỆU KHÓA NGOẠI (MOVIES & BRANCHES)
// =========================================================================

async function loadForeignKeys() {
    const defaultOption = '<option value="" selected hidden>--- Chọn ---</option>';

    // Hiển thị trạng thái tải
    branchCheckboxContainer.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải chi nhánh...';
    movieSelect.innerHTML = '<option value="" disabled selected hidden>Đang tải phim...</option>';

    try {
        // --- 1. Tải Danh sách Branch ---
        const branchResponse = await fetch(BRANCH_API_URL);
        const branches = await branchResponse.json();
        allBranches = Array.isArray(branches) ? branches : [];

        filterBranchSelect.innerHTML = '<option value="all" selected>Lọc theo Chi nhánh...</option>';
        branchCheckboxContainer.innerHTML = '';

        if (Array.isArray(allBranches)) {
            allBranches.forEach(branch => {
                const branchId = branch.branchId || branch.id;

                // ✅ LOGIC KIỂM TRA ISACTIVE LINH HOẠT (1/0 hoặc true/false)
                const isActive = branch.isActive === 1 || branch.isActive === true;

                if (branchId) {
                    const status = isActive ? '' : ' (Đã đóng)';
                    const optionTextFilter = branch.branchName + status;

                    const checkboxDiv = document.createElement('div');
                    checkboxDiv.className = 'form-check';
                    checkboxDiv.innerHTML = `
                        <input class="form-check-input branch-checkbox" type="checkbox" value="${branchId}" id="branch-${branchId}">
                        <label class="form-check-label" for="branch-${branchId}">
                            ${branch.branchName} ${status}
                        </label>
                    `;
                    branchCheckboxContainer.appendChild(checkboxDiv);

                    filterBranchSelect.appendChild(new Option(optionTextFilter, branchId));
                }
            });
        }

        // --- 2. Tải Danh sách Movie ---
        const movieResponse = await fetch(MOVIE_API_URL);
        const movieApiData = await movieResponse.json();
        allMovies = movieApiData.result || [];

        movieSelect.innerHTML = defaultOption;

        allMovies.forEach(movie => {
            const movieId = movie.movieID || movie.id;
            if (movieId) {
                movieSelect.appendChild(new Option(movie.title, movieId));
            }
        });

        movieSelect.value = "";

    } catch (error) {
        console.error('Lỗi khi tải khóa ngoại:', error);
        Swal.fire('Lỗi', 'Không thể tải danh sách Phim hoặc Chi nhánh. Kiểm tra API.', 'error');
        branchCheckboxContainer.innerHTML = 'Lỗi tải chi nhánh.';
        movieSelect.innerHTML = '<option value="" disabled selected hidden>Lỗi tải</option>';
    }
}


// =========================================================================
// HÀM TẢI & HIỂN THỊ DANH SÁCH (READ - CLIENT-SIDE PAGING)
// =========================================================================

/**
 * Tải dữ liệu khoảng chiếu.
 * @param {number} page - Trang cần hiển thị (0-based).
 * @param {boolean} loadFromApi - Nếu TRUE, gọi API để tải lại toàn bộ dữ liệu.
 */
async function loadPeriods(page = 0, loadFromApi = false) {
    currentPage = page;
    const branchIdFilter = filterBranchSelect.value;
    periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải dữ liệu...</td></tr>`;
    paginationFooter.innerHTML = '';

    // 1. Tải Dữ liệu gốc (chỉ tải khi khởi tạo hoặc thay đổi bộ lọc)
    if (loadFromApi) {
        let url = PERIOD_API_URL;
        if (branchIdFilter !== 'all') {
            // API này phải trả về TẤT CẢ (Active/Inactive) periods của branch đó.
            url = `${PERIOD_API_URL}/branch/${branchIdFilter}`;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('Failed to fetch periods');
            allPeriodsData = await response.json();
            currentPage = 0; // Reset về trang đầu khi tải lại toàn bộ
        } catch (error) {
            console.error('Lỗi tải danh sách:', error);
            periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-danger">Lỗi khi tải dữ liệu.</td></tr>`;
            return;
        }
    } else {
        currentPage = page;
    }

    // 2. Lọc Dữ liệu (Không lọc isActive ở đây, hiển thị tất cả để Admin quản lý)
    let periodsToProcess = allPeriodsData;

    // 3. Phân trang Dữ liệu (DÙNG JAVASCRIPT)
    const totalItems = periodsToProcess.length;
    const totalPages = Math.ceil(totalItems / PAGE_SIZE);

    const startIndex = currentPage * PAGE_SIZE;
    const endIndex = startIndex + PAGE_SIZE;

    const periodsToDisplay = periodsToProcess.slice(startIndex, endIndex);

    // 4. Hiển thị
    periodsBody.innerHTML = '';

    if (periodsToDisplay.length === 0) {
        periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted">Không tìm thấy khoảng thời gian chiếu nào.</td></tr>`;
        return;
    }

    periodsToDisplay.forEach(period => {

        const movie = allMovies.find(m => String(m.movieID || m.id) === String(period.movieId));
        const movieTitle = movie ? movie.title : period.movieTitle || `ID: ${period.movieId}`;
        const posterUrl = movie && movie.posterUrl ?
            movie.posterUrl :
            '../images/default-poster.png';

        // --- Xử lý trạng thái (ĐÃ SỬA LỖI ISACTIVE) ---
        // ✅ KIỂM TRA LINH HOẠT: chấp nhận cả 1 và true là 'Đang hoạt động'
        const isPeriodActive = period.isActive === 1 || period.isActive === true;

        const statusText = isPeriodActive ? 'Đang hoạt động' : 'Đã vô hiệu hóa';
        // ✅ ĐÃ SỬA MÀU: bg-success cho Active, bg-danger cho Inactive (như ảnh lỗi)
        const statusClass = isPeriodActive ? 'badge bg-success' : 'badge bg-danger';
        // -------------------------

        const row = periodsBody.insertRow();

        row.insertCell(0).textContent = period.id;
        row.insertCell(1).innerHTML = `<img src="${posterUrl}" alt="${movieTitle}" class="movie-poster-img" onerror="this.onerror=null;this.src='../images/default-poster.png';">`;
        row.insertCell(2).textContent = movieTitle;

        // Mapping Branch Name (dù API có trả về hay không)
        let branchNameDisplay = period.branchName || 'N/A';
        if (!period.branchName && period.branchId) {
            const branch = allBranches.find(b => String(b.branchId || b.id) === String(period.branchId));
            branchNameDisplay = branch ? branch.branchName : `ID: ${period.branchId}`;
        }
        row.insertCell(3).textContent = branchNameDisplay;

        row.insertCell(4).textContent = new Date(period.startDate).toLocaleDateString('vi-VN');
        row.insertCell(5).textContent = new Date(period.endDate).toLocaleDateString('vi-VN');

        // Cột Trạng Thái
        const statusCell = row.insertCell(6);
        statusCell.innerHTML = `<span class="${statusClass}">${statusText}</span>`;

        // Cột Hành động (Cột thứ 7 - index 7)
        row.insertCell(7).innerHTML = `
            <button class="btn btn-warning btn-sm me-2" onclick="editPeriod(${period.id})">
                <i class="fa-solid fa-pen-to-square"></i>
            </button>
            <button class="btn ${isPeriodActive ? 'btn-danger' : 'btn-success'} btn-sm" onclick="togglePeriodStatus(${period.id}, ${isPeriodActive})">
                 <i class="fa-solid fa-${isPeriodActive ? 'power-off' : 'check'}"></i>
            </button>
        `;

        // Lưu dữ liệu đầy đủ cho chức năng chỉnh sửa
        row.dataset.period = JSON.stringify({
            id: period.id,
            movieId: period.movieId,
            branchId: period.branchId,
            startDate: period.startDate,
            endDate: period.endDate,
            isActive: period.isActive
        });
    });

    // 5. Tạo footer phân trang
    renderPagination(totalPages, currentPage);
}


// =========================================================================
// HÀM HIỂN THỊ POSTER TRONG FORM
// =========================================================================

movieSelect.addEventListener('change', showMoviePosterPreview);

function showMoviePosterPreview() {
    const selectedMovieId = movieSelect.value;
    const movie = allMovies.find(m => String(m.movieID || m.id) === String(selectedMovieId));

    if (movie && movie.posterUrl) {
        posterPreviewImg.src = movie.posterUrl;
        posterPreviewContainer.style.display = 'block';
    } else {
        posterPreviewContainer.style.display = 'none';
        posterPreviewImg.src = '';
    }
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
            <a class="page-link" href="#" onclick="loadPeriods(${currentPage - 1}, false)">Trước</a>
        </li>
    `;

    // Hiển thị các nút số
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);

    if (totalPages > 5) {
        if (currentPage <= 2) endPage = 4;
        else if (currentPage >= totalPages - 3) startPage = totalPages - 5;
    } else {
        endPage = totalPages - 1;
    }

    for (let i = startPage; i <= endPage; i++) {
        paginationFooter.innerHTML += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadPeriods(${i}, false)">${i + 1}</a>
            </li>
        `;
    }

    // Nút Next
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPeriods(${currentPage + 1}, false)">Sau</a>
        </li>
    `;
}


// =========================================================================
// HÀM XỬ LÝ FORM (CREATE / UPDATE)
// =========================================================================

periodForm.addEventListener('submit', handleFormSubmission);

async function handleFormSubmission(e) {
    e.preventDefault();

    const id = periodID.value;
    const isUpdate = id !== '';

    const checkedCheckboxes = branchCheckboxContainer.querySelectorAll('.branch-checkbox:checked');
    // Chuyển giá trị về số nguyên cho API
    const selectedBranchIds = Array.from(checkedCheckboxes).map(checkbox => parseInt(checkbox.value));

    // --- 1. XỬ LÝ CẬP NHẬT (PUT) ---
    if (isUpdate) {
        if (selectedBranchIds.length !== 1) {
            Swal.fire('Lỗi', 'Khi cập nhật, bạn phải chọn DUY NHẤT một chi nhánh.', 'warning');
            return;
        }

        const url = `${PERIOD_API_URL}/${id}`;
        const requestBody = {
            movieId: parseInt(movieSelect.value),
            branchId: selectedBranchIds[0],
            startDate: startDateInput.value,
            endDate: endDateInput.value
        };

        try {
            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            });

            if (response.ok) {
                Swal.fire('Thành công!', 'Khoảng thời gian chiếu đã được cập nhật!', 'success');
                resetForm();
                loadPeriods(currentPage, true);
            } else {
                const errorData = await response.json().catch(() => ({ message: response.statusText }));
                Swal.fire('Thất bại', `Lỗi ${response.status} khi cập nhật: ${errorData.message || 'Kiểm tra dữ liệu đầu vào.'}`, 'error');
            }

        } catch (error) {
            console.error('Lỗi kết nối khi cập nhật:', error);
            Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ khi cập nhật.', 'error');
        }

        return;
    }

    // --- 2. XỬ LÝ TẠO MỚI (POST) ---
    if (selectedBranchIds.length === 0) {
        Swal.fire('Lỗi', 'Vui lòng chọn ít nhất một chi nhánh.', 'warning');
        return;
    }

    // Tương tự cho các kiểm tra khác (Ngày, Phim)

    const loadingPopup = Swal.fire({
        title: 'Đang Tạo...',
        html: `Đang gửi **${selectedBranchIds.length}** yêu cầu tạo khoảng chiếu.<br>Vui lòng chờ...`,
        allowOutsideClick: false,
        didOpen: () => { Swal.showLoading(); }
    });

    let successCount = 0;
    let failCount = 0;
    const allRequests = selectedBranchIds.map(branchId => {
        const requestBody = {
            movieId: parseInt(movieSelect.value),
            branchId: branchId,
            startDate: startDateInput.value,
            endDate: endDateInput.value,
        };

        return fetch(PERIOD_API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody),
        })
            .then(response => {
                if (response.ok || response.status === 201) {
                    successCount++;
                } else {
                    failCount++;
                }
                return { status: response.ok ? 'success' : 'fail', branchId: branchId };
            })
            .catch(error => {
                console.error(`Lỗi tạo cho Branch ID ${branchId}:`, error);
                failCount++;
                return { status: 'error', branchId: branchId };
            });
    });

    await Promise.all(allRequests);

    loadingPopup.close();

    const totalRequests = selectedBranchIds.length;
    if (successCount > 0) {
        Swal.fire({
            title: 'Thành công!',
            html: `Đã tạo thành công **${successCount}/${totalRequests}** khoảng chiếu.`,
            icon: 'success',
        });
    } else {
        Swal.fire({
            title: 'Thất bại toàn bộ!',
            html: `Không thể tạo bất kỳ khoảng chiếu nào. Vui lòng kiểm tra API và dữ liệu.`,
            icon: 'error',
        });
    }

    resetForm();
    loadPeriods(0, true);
}


// =========================================================================
// HÀM HỖ TRỢ FORM (EDIT, DELETE, RESET)
// =========================================================================

window.editPeriod = function(id) {
    const period = allPeriodsData.find(p => p.id === id);
    if (!period) {
        Swal.fire('Lỗi', `Không tìm thấy khoảng chiếu ID: ${id} trong bộ nhớ.`, 'error');
        return;
    }

    // 1. Cập nhật Form
    periodID.value = period.id;
    movieSelect.value = String(period.movieId);
    startDateInput.value = period.startDate.slice(0, 10);
    endDateInput.value = period.endDate.slice(0, 10);
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-3"></i> Chỉnh Sửa Khoảng Chiếu #${period.id}`;

    // 2. Xử lý Chi Nhánh (Chỉ chọn 1 và disable các nút khác)
    branchCheckboxContainer.querySelectorAll('.branch-checkbox').forEach(checkbox => {
        checkbox.disabled = true;
        checkbox.checked = false;
        if (String(checkbox.value) === String(period.branchId)) {
            checkbox.checked = true;
        }
    });
    branchHint.style.display = 'none';

    // 3. Hiển thị Poster
    showMoviePosterPreview();

    // 4. Cập nhật nút
    submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i>Cập Nhật';
    submitBtn.classList.replace('btn-primary', 'btn-warning');
    cancelBtn.style.display = 'inline-block';

    periodForm.scrollIntoView({ behavior: 'smooth' });
}

function resetForm() {
    periodForm.reset();
    periodID.value = '';

    formTitle.innerHTML = '<i class="fa-solid fa-calendar-plus me-3"></i>Thêm Khoảng Thời Gian Chiếu';
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i>Tạo Khoảng Chiếu';
    submitBtn.classList.replace('btn-warning', 'btn-primary');
    cancelBtn.style.display = 'none';

    // BẬT LẠI CHẾ ĐỘ TẠO MỚI
    branchCheckboxContainer.querySelectorAll('.branch-checkbox').forEach(checkbox => {
        checkbox.disabled = false;
        checkbox.checked = false;
    });
    branchHint.style.display = 'inline-block';

    // Ẩn Poster Preview
    posterPreviewContainer.style.display = 'none';
    posterPreviewImg.src = '';

    movieSelect.value = "";
    startDateInput.valueAsDate = new Date();
}

cancelBtn.addEventListener('click', resetForm);


// 🔥 HÀM XỬ LÝ CHUYỂN TRẠNG THÁI (TOGGLE ISACTIVE) - THAY CHO DELETE CŨ
window.togglePeriodStatus = async function(id, currentIsActive) {
    const action = currentIsActive ? 'Vô hiệu hóa' : 'Kích hoạt lại';
    const newIsActive = !currentIsActive;

    // API có thể yêu cầu method DELETE (khi vô hiệu hóa) hoặc PUT (khi kích hoạt lại)
    // Hoặc API có endpoint riêng cho việc này (ví dụ: /toggle-status)
    // API tạm thời giả định: Dùng DELETE cho vô hiệu hóa/xóa, dùng PUT cho kích hoạt lại

    // Nếu API của bạn chỉ hỗ trợ 'DELETE' để VÔ HIỆU HÓA (soft delete),
    // chúng ta sẽ cần endpoint riêng cho KÍCH HOẠT LẠI.
    // Tạm thời, tôi sẽ dùng DELETE/PUT dựa trên trạng thái.

    if (currentIsActive) {
        // --- LOGIC XÓA/VÔ HIỆU HÓA (DÙNG DELETE) ---
        const period = allPeriodsData.find(p => p.id === id);
        if (!period) return;
        const today = new Date().toISOString().split('T')[0];
        const isFuture = period.startDate > today;

        const result = await Swal.fire({
            title: "Xác nhận vô hiệu hóa?",
            html: isFuture
                ? "Khoảng chiếu **chưa bắt đầu**. Chọn Xóa Cứng sẽ xóa vĩnh viễn khỏi hệ thống."
                : "Khoảng chiếu đang/đã diễn ra. Chọn Vô Hiệu Hóa sẽ đặt `isActive=false` (Soft Delete).",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: isFuture ? "#dc3545" : "#ffc107",
            cancelButtonColor: "#6c757d",
            confirmButtonText: isFuture ? "Xóa Cứng Vĩnh Viễn" : "Vô Hiệu Hóa"
        });

        if (result.isConfirmed) {
            try {
                const response = await fetch(`${PERIOD_API_URL}/${id}`, { method: 'DELETE' });

                if (response.ok || response.status === 204) {
                    Swal.fire({
                        icon: "success",
                        title: isFuture ? "Đã Xóa Cứng!" : "Đã Vô Hiệu Hóa!",
                        text: isFuture
                            ? "Khoảng thời gian chiếu đã được xóa hoàn toàn."
                            : "Khoảng thời gian chiếu đã được vô hiệu hóa thành công.",
                    });
                    loadPeriods(currentPage, true);
                } else {
                    const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
                    Swal.fire('Thất bại', `Lỗi ${response.status}: ${errorData.message || 'Không thể thực hiện thao tác.'}`, 'error');
                }
            } catch (error) {
                console.error('Lỗi kết nối hoặc xử lý:', error);
                Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ.', 'error');
            }
        }

    } else {
        // --- LOGIC KÍCH HOẠT LẠI (GIẢ ĐỊNH DÙNG PUT) ---

        const result = await Swal.fire({
            title: `Xác nhận Kích hoạt lại?`,
            text: "Khoảng chiếu này sẽ được đặt lại trạng thái 'Đang hoạt động'.",
            icon: "question",
            showCancelButton: true,
            confirmButtonColor: '#198754', // Màu xanh lá cho success
            cancelButtonColor: "#6c757d",
            confirmButtonText: "Kích hoạt",
        });

        if (result.isConfirmed) {
            try {
                // API phải hỗ trợ cập nhật isActive qua PUT
                const period = allPeriodsData.find(p => p.id === id);
                const url = `${PERIOD_API_URL}/${id}`;
                const requestBody = {
                    movieId: period.movieId,
                    branchId: period.branchId,
                    startDate: period.startDate.slice(0, 10),
                    endDate: period.endDate.slice(0, 10),
                    isActive: newIsActive ? 1 : 0 // Gửi lại dữ liệu cũ, chỉ thay đổi isActive
                };

                const response = await fetch(url, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestBody),
                });

                if (response.ok) {
                    Swal.fire('Thành công!', 'Khoảng chiếu đã được kích hoạt lại.', 'success');
                    loadPeriods(currentPage, true);
                } else {
                    Swal.fire('Thất bại', `Lỗi khi kích hoạt lại: ${response.status}`, 'error');
                }
            } catch (error) {
                console.error('Lỗi kết nối khi kích hoạt lại:', error);
                Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ khi kích hoạt lại.', 'error');
            }
        }
    }
}


// =========================================================================
// GẮN SỰ KIỆN & KHỞI TẠO
// =========================================================================

// Gắn sự kiện cho bộ lọc: Tải lại dữ liệu (true) khi thay đổi bộ lọc
filterBranchSelect.addEventListener('change', () => loadPeriods(0, true));


document.addEventListener('DOMContentLoaded', async () => {
    // Tải các khóa ngoại trước
    await loadForeignKeys();
    // Tải danh sách khoảng chiếu lần đầu (loadFromApi = true)
    loadPeriods(0, true);

    // Đặt ngày hiện tại làm ngày bắt đầu mặc định
    startDateInput.valueAsDate = new Date();
});