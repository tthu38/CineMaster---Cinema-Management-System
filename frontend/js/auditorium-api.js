const API_BASE_URL = 'http://localhost:8080';
const API_URL = API_BASE_URL + '/api/v1/auditoriums';
const BRANCH_API_URL = API_BASE_URL + '/api/v1/branches';
const API_AUDITORIUM_BY_BRANCH = API_URL + '/branch';

// Lấy các phần tử DOM
const auditoriumForm = document.getElementById('auditorium-form');
const auditoriumsBody = document.getElementById('auditoriums-body');
const loadButton = document.getElementById('load-auditoriums');
const formTitle = document.getElementById('form-title');
const submitBtn = document.getElementById('submit-btn');
const cancelBtn = document.getElementById('cancel-btn');
const auditoriumIdField = document.getElementById('auditoriumID');
const paginationControls = document.getElementById('pagination-controls');

// Các Element Dropdown
const branchSelect = document.getElementById('branchID');
const filterBranchSelect = document.getElementById('filterBranchID');

// Biến cờ và phân trang
let isBranchesLoaded = false;
let allAuditoriumsData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;


// --- HÀM TẠO CÁC NÚT PHÂN TRANG (Giữ nguyên) ---
function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = '';
    if (totalPages <= 1) return;

    const ul = document.createElement('ul');
    ul.className = 'pagination pagination-sm';

    const createPageLink = (text, pageNumber, isDisabled = false, isCurrent = false) => {
        const li = document.createElement('li');
        li.className = `page-item ${isDisabled ? 'disabled' : ''} ${isCurrent ? 'active' : ''}`;
        const a = document.createElement('a');
        a.href = '#';
        a.className = 'page-link';
        a.innerHTML = text;

        if (!isDisabled) {
            a.onclick = (e) => {
                e.preventDefault();
                displayAuditoriums(pageNumber);
            };
        }
        li.appendChild(a);
        return li;
    };

    ul.appendChild(createPageLink('&laquo;', currentPage - 1, currentPage === 1));

    const maxPagesToShow = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);

    if (endPage - startPage + 1 < maxPagesToShow) {
        startPage = Math.max(1, endPage - maxPagesToShow + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageLink(i, i, false, i === currentPage));
    }

    ul.appendChild(createPageLink('&raquo;', currentPage + 1, currentPage === totalPages));
    paginationControls.appendChild(ul);
}


// --- HÀM HIỂN THỊ DỮ LIỆU ĐÃ PHÂN TRANG (Giữ nguyên) ---
function displayAuditoriums(page = 1) {
    auditoriumsBody.innerHTML = '';

    if (allAuditoriumsData.length === 0) {
        auditoriumsBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Không tìm thấy phòng chiếu nào.</td></tr>';
        paginationControls.innerHTML = '';
        return;
    }

    const totalItems = allAuditoriumsData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

    if (page < 1) page = 1;
    if (page > totalPages) page = totalPages;
    currentPage = page;

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const auditoriumsToDisplay = allAuditoriumsData.slice(start, end);

    renderPaginationControls(totalPages);

    auditoriumsToDisplay.forEach(auditorium => {
        const row = auditoriumsBody.insertRow();
        row.insertCell(0).textContent = auditorium.auditoriumID;
        row.insertCell(1).textContent = auditorium.name;
        row.insertCell(2).textContent = auditorium.branchName;
        row.insertCell(3).textContent = auditorium.type;
        row.insertCell(4).textContent = auditorium.capacity;

        // Cột Trạng Thái
        const statusCell = row.insertCell(5);
        const statusText = auditorium.isActive ? 'Hoạt động' : 'Đã đóng';
        const statusClass = auditorium.isActive ? 'text-success fw-bold' : 'text-danger';
        statusCell.innerHTML = `<span class="${statusClass}">${statusText}</span>`;

        const actionsCell = row.insertCell(6);

        const editBtn = document.createElement('button');
        editBtn.textContent = 'Sửa';
        editBtn.className = 'btn btn-warning btn-sm me-2 edit-btn';
        editBtn.onclick = () => populateFormForUpdate(auditorium);
        actionsCell.appendChild(editBtn);

        // Nút Toggle Trạng Thái
        const toggleBtn = document.createElement('button');
        const toggleText = auditorium.isActive ? 'Đóng' : 'Mở lại';
        toggleBtn.textContent = toggleText;
        toggleBtn.className = `btn btn-sm ${auditorium.isActive ? 'btn-danger delete-btn' : 'btn-info'}`;
        // 🔥 Lỗi 403 xảy ra ở đây: BẠN CẦN CHỈ ĐỊNH API SẼ ĐƯỢC GỌI
        toggleBtn.onclick = () => toggleAuditoriumStatus(auditorium.auditoriumID, !auditorium.isActive);
        actionsCell.appendChild(toggleBtn);
    });
}


// --- HÀM TẢI DANH SÁCH CHI NHÁNH VÀO DROPDOWN (Giữ nguyên) ---
async function loadBranches() {
    if (isBranchesLoaded) return;

    branchSelect.innerHTML = '<option value="" disabled selected>Đang tải chi nhánh...</option>';
    filterBranchSelect.innerHTML = '<option value="" disabled selected>Đang tải...</option>';

    try {
        const response = await fetch(BRANCH_API_URL);
        if (!response.ok) {
            throw new Error('Không thể tải danh sách chi nhánh.');
        }
        const branches = await response.json();

        // 1. Chuẩn bị dropdown FORM (branchID) - Lấy TẤT CẢ Chi nhánh
        branchSelect.innerHTML = '<option value="" disabled selected hidden>--- Chọn Chi Nhánh (Cần chú ý trạng thái) ---</option>';

        // 2. Chuẩn bị dropdown LỌC (filterBranchID) - LẤY TẤT CẢ Chi nhánh
        filterBranchSelect.innerHTML = '<option value="">--- Tất Cả Chi Nhánh ---</option>';

        if (branches.length === 0) {
            const noBranchOption = '<option value="" disabled>Không có chi nhánh nào.</option>';
            branchSelect.innerHTML = noBranchOption;
            filterBranchSelect.innerHTML = noBranchOption;
            return;
        }

        branches.forEach(branch => {
            const optionValue = branch.branchId;
            // Hiển thị rõ ràng chi nhánh nào Đã đóng
            const statusLabel = branch.isActive ? '' : ' (Đã đóng ⚠️)';
            const optionText = branch.branchName + statusLabel;

            // Thêm vào dropdown LỌC
            const filterOption = new Option(optionText, optionValue);
            filterBranchSelect.appendChild(filterOption);

            // Thêm vào dropdown FORM (branchID) - Cho phép chọn cả chi nhánh đang đóng
            const createOption = new Option(optionText, optionValue);
            branchSelect.appendChild(createOption);

            // Tùy chọn: Thêm CSS cảnh báo cho chi nhánh đang đóng
            if (!branch.isActive) {
                createOption.style.color = '#ff9999';
            }
        });

        isBranchesLoaded = true;

    } catch (error) {
        console.error('Lỗi khi tải chi nhánh:', error);
        const errorOption = '<option value="" disabled selected>Lỗi tải chi nhánh.</option>';
        branchSelect.innerHTML = errorOption;
        filterBranchSelect.innerHTML = errorOption;
    }
}


// --- HÀM TẢI AUDITORIUMS (READ ALL / LỌC) (Giữ nguyên) ---
async function loadAuditoriums() {
    await loadBranches();

    auditoriumsBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Đang tải danh sách...</td></tr>';
    paginationControls.innerHTML = '';

    let url;
    const selectedBranchId = filterBranchSelect.value;

    // Lọc theo tất cả chi nhánh (bao gồm cả chi nhánh đã đóng)
    if (selectedBranchId && selectedBranchId !== "") {
        url = `${API_AUDITORIUM_BY_BRANCH}/${selectedBranchId}`;
    } else {
        url = API_URL; // Lấy TẤT CẢ Auditorium
    }

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        allAuditoriumsData = await response.json();
        displayAuditoriums(1);

    } catch (error) {
        console.error('Lỗi khi tải danh sách phòng chiếu:', error);
        Swal.fire('Lỗi Kết Nối', 'Không thể tải dữ liệu phòng chiếu từ server.', 'error');
        auditoriumsBody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:var(--red)">Lỗi kết nối: ${error.message}</td></tr>`;
        paginationControls.innerHTML = '';
    }
}

// --- HÀM TẠO/CẬP NHẬT (Giữ nguyên) ---
async function handleFormSubmission(e) {
    e.preventDefault();

    if (branchSelect.value === "") {
        Swal.fire('Lỗi Dữ Liệu', 'Vui lòng chọn một Chi Nhánh.', 'warning');
        return;
    }

    const id = auditoriumIdField.value;
    const isUpdate = id !== '';
    const method = isUpdate ? 'PUT' : 'POST';
    const url = isUpdate ? `${API_URL}/${id}` : API_URL;

    const requestBody = {
        name: document.getElementById('auditoriumName').value,
        capacity: parseInt(document.getElementById('capacity').value),
        type: document.getElementById('type').value,
        branchID: parseInt(branchSelect.value)
    };

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody),
        });

        if (response.ok || response.status === 201) {
            Swal.fire({
                title: 'Thành công!',
                text: `Phòng chiếu đã được ${isUpdate ? 'cập nhật' : 'tạo'} thành công!`,
                icon: 'success',
                timer: 2000
            });

            resetForm();
            loadAuditoriums();
        } else {
            const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
            console.error(errorData);

            let errorMessage = `Lỗi ${response.status}: ${errorData.message || errorData.error || 'Vui lòng kiểm tra ID Chi Nhánh hoặc dữ liệu đầu vào.'}`;

            if (response.status === 400 && errorData.errors && Array.isArray(errorData.errors)) {
                errorMessage += '<br><br>Chi tiết lỗi: <ul>' + errorData.errors.map(err => `<li>${err.defaultMessage || err.field}</li>`).join('') + '</ul>';
            }

            Swal.fire('Thất bại', errorMessage, 'error');
        }
    } catch (error) {
        console.error('Lỗi kết nối hoặc xử lý:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ.', 'error');
    }
}


// ------------------------------------------------------------------
// 🔥 ĐÃ SỬA: HÀM ĐÓNG/MỞ LẠI PHÒNG CHIẾU SỬ DỤNG DELETE VÀ POST
// ------------------------------------------------------------------
async function toggleAuditoriumStatus(id, newStatus) {
    let action = newStatus ? 'mở lại' : 'đóng';
    let url;
    let method;

    // Dựa vào newStatus để chọn phương thức và URL chính xác:
    if (newStatus) {
        // Nếu newStatus = TRUE: MỞ LẠI -> Dùng POST /activate
        url = `${API_URL}/${id}/activate`;
        method = 'POST';
    } else {
        // Nếu newStatus = FALSE: ĐÓNG -> Dùng DELETE (Deactivate)
        url = `${API_URL}/${id}`;
        method = 'DELETE';
    }

    const confirmText = newStatus ? 'Đúng, MỞ LẠI nó!' : 'Đúng, ĐÓNG nó!';
    const icon = newStatus ? 'info' : 'warning';

    const branchId = allAuditoriumsData.find(a => a.auditoriumID === id)?.branchID;
    const branchOption = filterBranchSelect.querySelector(`option[value="${branchId}"]`);
    const isBranchInactive = branchOption && branchOption.textContent.includes('(Đã đóng ⚠️)');

    let warningHtml = `Bạn sẽ **${action}** phòng chiếu **ID: ${id}**.`;

    if (newStatus && isBranchInactive) {
        warningHtml += `<br><br>🚨 **CẢNH BÁO:** Chi nhánh này đang ở trạng thái **Đã đóng** trên hệ thống. Mở lại phòng chiếu sẽ không làm phòng xuất hiện trên trang khách hàng cho đến khi chi nhánh được mở lại.`;
    }

    const result = await Swal.fire({
        title: `Xác nhận ${action} phòng chiếu?`,
        html: warningHtml,
        icon: icon,
        showCancelButton: true,
        confirmButtonText: confirmText,
        cancelButtonText: 'Hủy bỏ',
        reverseButtons: true,
        customClass: {
            confirmButton: `btn btn-${newStatus ? 'btn-info' : 'btn-danger'} me-3`,
            cancelButton: 'btn btn-secondary'
        },
        buttonsStyling: false
    });

    if (!result.isConfirmed) return;

    try {
        // Gửi yêu cầu DELETE hoặc POST /activate
        const response = await fetch(url, {
            method: method,
            // DELETE/POST /activate không cần body, nhưng vẫn cần Content-Type
            headers: { 'Content-Type': 'application/json' },
            // KHÔNG cần body cho DELETE hoặc POST /activate theo Controller của bạn
            body: null
        });

        if (response.ok || response.status === 204) { // 204 No Content là response của DELETE/POST thành công
            Swal.fire({
                title: 'Thành công!',
                text: `Phòng chiếu ID ${id} đã được ${action} thành công.`,
                icon: 'success'
            });
            loadAuditoriums();
        } else if (response.status === 403) {
            Swal.fire('Thất bại', 'Lỗi 403 Forbidden: Yêu cầu của bạn bị từ chối do vấn đề bảo mật (Security Config).', 'error');
        } else {
            const errorText = await response.text();
            Swal.fire('Thất bại', `Lỗi ${response.status}: ${errorText || 'Không xác định.'}`, 'error');
        }
    } catch (error) {
        console.error(`Lỗi khi ${action} phòng chiếu:`, error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ. Vui lòng kiểm tra Server Log.', 'error');
    }
}
// ------------------------------------------------------------------


// --- HÀM ĐIỀN DỮ LIỆU VÀO FORM (Giữ nguyên) ---
function populateFormForUpdate(auditorium) {
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập Nhật Phòng Chiếu (ID: ${auditorium.auditoriumID})`;
    submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i> Lưu Cập Nhật';
    cancelBtn.style.display = 'inline-block';

    auditoriumIdField.value = auditorium.auditoriumID;
    document.getElementById('auditoriumName').value = auditorium.name;
    document.getElementById('capacity').value = auditorium.capacity;
    document.getElementById('type').value = document.getElementById('type').value;

    branchSelect.value = auditorium.branchID.toString();

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// --- HÀM ĐẶT LẠI FORM (Giữ nguyên) ---
function resetForm() {
    auditoriumForm.reset();
    auditoriumIdField.value = '';
    formTitle.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Thêm Phòng Chiếu Mới';
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Tạo Phòng Chiếu';
    cancelBtn.style.display = 'none';
}


// --- GẮN SỰ KIỆN (Giữ nguyên) ---
auditoriumForm.addEventListener('submit', handleFormSubmission);
loadButton.addEventListener('click', loadAuditoriums);
cancelBtn.addEventListener('click', resetForm);
filterBranchSelect.addEventListener('change', loadAuditoriums);


// Tải danh sách khi trang được load lần đầu
document.addEventListener('DOMContentLoaded', loadAuditoriums);
