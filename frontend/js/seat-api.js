// =========================================================================
// KHAI BÁO CẤU HÌNH VÀ BIẾN TOÀN CỤC
// =========================================================================

const API_BASE_URL = 'http://localhost:8080';
const SEAT_API_URL = API_BASE_URL + '/api/v1/seats';
const AUDITORIUM_API_URL = API_BASE_URL + '/api/v1/auditoriums';
const SEATTYPE_API_URL = API_BASE_URL + '/api/v1/seattypes';

// Biến toàn cục để lưu trữ dữ liệu đã tải và danh sách Chi nhánh
let allAuditoriums = [];
let allBranches = [];
let allSeatsData = [];
let isInitialLoadDone = false; // Biến CỜ quan trọng để chống reset dropdown

// --- BIẾN PHÂN TRANG (PAGINATION) ---
let currentPage = 1;
const ITEMS_PER_PAGE = 10; // Số ghế tối đa hiển thị trên mỗi trang

// Lấy các phần tử DOM (CRUD đơn lẻ)
const seatForm = document.getElementById('seat-form');
const seatsBody = document.getElementById('seats-body');
const loadButton = document.getElementById('load-seats');
const formTitle = document.getElementById('form-title');
const submitBtn = document.getElementById('submit-btn');
const cancelBtn = document.getElementById('cancel-btn');
const seatIdField = document.getElementById('seatID');

// Dropdowns (CRUD đơn lẻ)
const auditoriumSelect = document.getElementById('auditoriumID');
const seatTypeSelect = document.getElementById('typeID');

// --- CÁC PHẦN TỬ DÀNH CHO TẠO HÀNG LOẠT ---
const bulkSeatForm = document.getElementById('bulk-seat-form');
const bulkAuditoriumSelect = document.getElementById('bulkAuditoriumID');
const bulkSeatTypeSelect = document.getElementById('bulkTypeID');
const rowCountInput = document.getElementById('rowCount');
const columnCountInput = document.getElementById('columnCount');
const totalSeatsSpan = document.getElementById('totalSeats');

// --- CÁC PHẦN TỬ DÀNH CHO CẬP NHẬT HÀNG LOẠT ---
const bulkUpdateForm = document.getElementById('bulk-update-form');
const updateAuditoriumSelect = document.getElementById('updateAuditoriumID');
const newTypeSelect = document.getElementById('newTypeID');
const rowToUpdateInput = document.getElementById('rowToUpdate');
const newStatusSelect = document.getElementById('newStatusSelect');
const isConvertCoupleSeatCheckbox = document.getElementById('isConvertCoupleSeat');
const isSeparateCoupleSeatCheckbox = document.getElementById('isSeparateCoupleSeat');

// --- CÁC PHẦN TỬ DÀNH CHO SƠ ĐỒ GHẾ ---
const diagramAuditoriumSelect = document.getElementById('diagramAuditoriumID');
const seatDiagram = document.getElementById('seat-diagram');

// --- KHAI BÁO CÁC DROPDOWN CHI NHÁNH ---
const diagramBranchSelect = document.getElementById('diagramBranchID');
const singleBranchSelect = document.getElementById('singleBranchID');
const bulkBranchSelect = document.getElementById('bulkBranchID');
const updateBranchSelect = document.getElementById('updateBranchID');

// --- KHAI BÁO CÁC PHẦN TỬ PHÂN TRANG ---
const paginationControls = document.getElementById('pagination-controls');

// --- KHAI BÁO BIẾN CHO NÚT XÓA TRÊN FORM SỬA (CHỈ CÒN ĐỂ CHECK NULL) ---
const formActionsContainer = document.querySelector('#seat-form .d-flex.justify-content-end');
let deleteBtnOnForm = null;

// =========================================================================
// LOGIC MAPPING VÀ CÁC HÀM TIỆN ÍCH
// =========================================================================

const SEAT_TYPE_CLASSES = {
    // CẦN ĐẢM BẢO TypeID (1, 2, 3...) khớp với CSDL của bạn!
    1: 'seat-type-normal',
    2: 'seat-type-vip',
    3: 'seat-type-couple',
};

const SEAT_STATUS_CLASSES = {
    'BROKEN': 'seat-broken',
    'RESERVED': 'seat-reserved',
};

function getSeatClass(seat) {
    let classes = [];
    const typeClass = SEAT_TYPE_CLASSES[seat.typeID];
    if (typeClass) {
        classes.push(typeClass);
    }
    const statusClass = SEAT_STATUS_CLASSES[seat.status.toUpperCase()];
    if (statusClass) {
        classes.push(statusClass);
    }
    return classes.join(' ');
}

// =========================================================================
// HÀM LỌC VÀ HIỂN THỊ DỮ LIỆU
// =========================================================================

function renderAuditoriumOptions(selectElement, branchName) {
    const filteredAuditoriums = branchName
        ? allAuditoriums.filter(aud => aud.branchName === branchName)
        : allAuditoriums;

    const defaultOption = '<option value="" disabled selected hidden>--- Chọn Phòng Chiếu ---</option>';
    selectElement.innerHTML = defaultOption;

    if (filteredAuditoriums.length === 0 && branchName) {
        selectElement.innerHTML += '<option value="" disabled>Không có Phòng Chiếu cho chi nhánh này.</option>';
        return;
    }

    filteredAuditoriums.forEach(aud => {
        // ĐÃ SỬA: Loại bỏ ID khỏi text hiển thị (Chỉ còn Tên)
        const textContent = `${aud.name}`;
        selectElement.appendChild(new Option(textContent, aud.auditoriumID));
    });
}

// --- HÀM THIẾT LẬP LỌC CHI NHÁNH/PHÒNG CHIẾU (ĐÃ FIX LỖI) ---
function setupAuditoriumFilter(branchSelect, auditoriumSelect) {
    const defaultBranchOption = '<option value="" disabled selected hidden>--- Chọn Chi nhánh ---</option>';

    // 1. Điền options Chi nhánh vào dropdown
    // Lưu ý: allBranches chỉ chứa tên, nên ID không bị hiển thị ở đây
    branchSelect.innerHTML = defaultBranchOption + allBranches.map(name => `<option value="${name}">${name}</option>`).join('');

    // Đảm bảo không bị disabled
    branchSelect.disabled = false;

    // 2. Gán sự kiện thay đổi (CHỈ GÁN 1 LẦN)
    branchSelect.addEventListener('change', (e) => {
        renderAuditoriumOptions(auditoriumSelect, e.target.value);
        auditoriumSelect.focus();
    });

    // 3. Khởi tạo lần đầu: Phòng Chiếu trống
    renderAuditoriumOptions(auditoriumSelect, null);
}


// --- HÀM TẢI TOÀN BỘ DỮ LIỆU GHẾ ---
async function loadAllSeatsData() {
    try {
        const response = await fetch(SEAT_API_URL);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        allSeatsData = await response.json();
    } catch (error) {
        console.error('Lỗi khi tải toàn bộ dữ liệu ghế:', error);
        Swal.fire('Lỗi Kết Nối', 'Không thể tải dữ liệu ghế ngồi từ server.', 'error');
        allSeatsData = [];
    }
}


// --- HÀM TẢI DANH SÁCH KHÓA NGOẠI (FIX LỖI RESET DROPDOWN) ---
async function loadForeignKeys() {
    const loadingOption = '<option value="" disabled selected>Đang tải...</option>';

    // --- Tải danh sách Phòng Chiếu và phân nhóm ---
    try {
        // LƯU TRẠNG THÁI HIỆN TẠI TRƯỚC KHI TẢI DỮ LIỆU MỚI
        const diagramAuditoriumID = diagramAuditoriumSelect.value;
        const diagramBranchName = diagramBranchSelect.value;
        const singleAuditoriumID = auditoriumSelect.value;
        const singleBranchName = singleBranchSelect.value;

        const audResponse = await fetch(AUDITORIUM_API_URL);
        allAuditoriums = await audResponse.json();
        allBranches = [...new Set(allAuditoriums.map(aud => aud.branchName))].filter(n => n);

        if (!isInitialLoadDone) {
            // KHỞI TẠO LẦN ĐẦU: GÁN SỰ KIỆN CHO TẤT CẢ DROPDOWN
            [auditoriumSelect, bulkAuditoriumSelect, updateAuditoriumSelect, diagramAuditoriumSelect,
                diagramBranchSelect, singleBranchSelect, bulkBranchSelect, updateBranchSelect].forEach(s => s.innerHTML = loadingOption);

            setupAuditoriumFilter(diagramBranchSelect, diagramAuditoriumSelect);
            setupAuditoriumFilter(singleBranchSelect, auditoriumSelect);
            setupAuditoriumFilter(bulkBranchSelect, bulkAuditoriumSelect);
            setupAuditoriumFilter(updateBranchSelect, updateAuditoriumSelect);
            isInitialLoadDone = true;
        } else {
            // CẬP NHẬT LẠI DỮ LIỆU OPTIONS VÀ KHÔI PHỤC GIÁ TRỊ ĐÃ CHỌN
            const pairs = [
                { branchSelect: diagramBranchSelect, audSelect: diagramAuditoriumSelect, branchName: diagramBranchName, audId: diagramAuditoriumID },
                { branchSelect: singleBranchSelect, audSelect: auditoriumSelect, branchName: singleBranchName, audId: singleAuditoriumID },
                // Thêm các cặp khác nếu cần giữ trạng thái
                { branchSelect: bulkBranchSelect, audSelect: bulkAuditoriumSelect, branchName: bulkBranchSelect.value, audId: bulkAuditoriumSelect.value },
                { branchSelect: updateBranchSelect, audSelect: updateAuditoriumSelect, branchName: updateBranchSelect.value, audId: updateAuditoriumSelect.value }
            ];

            pairs.forEach(pair => {
                const defaultBranchOption = '<option value="" disabled selected hidden>--- Chọn Chi nhánh ---</option>';

                // Cập nhật options Chi nhánh
                pair.branchSelect.innerHTML = defaultBranchOption + allBranches.map(name => `<option value="${name}">${name}</option>`).join('');

                if (pair.branchName) {
                    pair.branchSelect.value = pair.branchName; // Khôi phục chi nhánh

                    renderAuditoriumOptions(pair.audSelect, pair.branchName);

                    // Chỉ khôi phục AuditoriumID nếu nó thuộc chi nhánh hiện tại
                    if(allAuditoriums.find(aud => aud.auditoriumID === parseInt(pair.audId) && aud.branchName === pair.branchName)){
                        pair.audSelect.value = pair.audId; // Khôi phục phòng chiếu
                    } else {
                        pair.audSelect.value = ""; // Reset nếu ID cũ không còn hợp lệ
                    }

                } else {
                    renderAuditoriumOptions(pair.audSelect, null);
                }
                pair.branchSelect.disabled = false; // Đảm bảo mở khóa
            });
        }

    } catch (error) {
        console.error('Lỗi khi tải Phòng Chiếu:', error);
        const errorOption = '<option value="" disabled selected>Lỗi tải Phòng Chiếu.</option>';
        [auditoriumSelect, bulkAuditoriumSelect, updateAuditoriumSelect, diagramAuditoriumSelect].forEach(s => s.innerHTML = errorOption);
    }

    // --- Tải danh sách SeatType (ĐÃ SỬA) ---
    [seatTypeSelect, bulkSeatTypeSelect, newTypeSelect].forEach(s => s.innerHTML = loadingOption);
    try {
        const typeResponse = await fetch(SEATTYPE_API_URL);
        const seatTypes = await typeResponse.json();

        const defaultOption = '<option value="" disabled selected hidden>--- Chọn Loại Ghế ---</option>';
        const updateDefault = '<option value="" selected>--- Không thay đổi Loại ghế ---</option>';

        seatTypeSelect.innerHTML = defaultOption;
        bulkSeatTypeSelect.innerHTML = defaultOption;
        newTypeSelect.innerHTML = updateDefault;

        seatTypes.forEach(type => {
            // ĐÃ SỬA: Loại bỏ hệ số nhân giá và ID, chỉ hiển thị Tên Loại Ghế
            const textContent = `${type.typeName}`;

            seatTypeSelect.appendChild(new Option(textContent, type.typeID));
            bulkSeatTypeSelect.appendChild(new Option(textContent, type.typeID));
            newTypeSelect.appendChild(new Option(textContent, type.typeID));
        });
    } catch (error) {
        console.error('Lỗi khi tải Loại Ghế:', error);
        const errorOption = '<option value="" disabled selected>Lỗi tải Loại Ghế.</option>';
        [seatTypeSelect, bulkSeatTypeSelect, newTypeSelect].forEach(s => s.innerHTML = errorOption);
    }
}


// --- HÀM TẠO CÁC NÚT PHÂN TRANG ---
function renderPaginationControls(totalPages) {
    const paginationControls = document.getElementById('pagination-controls');
    paginationControls.innerHTML = '';

    if (totalPages <= 1) return;

    // Sử dụng class Bootstrap pagination
    const ul = document.createElement('ul');
    ul.className = 'pagination pagination-sm';

    // Hàm tạo link trang với CSS tùy chỉnh
    const createPageLink = (text, pageNumber, isDisabled = false, isCurrent = false) => {
        const li = document.createElement('li');
        // Vẫn dùng Bootstrap classes để quản lý trạng thái disabled/active
        li.className = `page-item ${isDisabled ? 'disabled' : ''} ${isCurrent ? 'active' : ''}`;

        const a = document.createElement('a');
        a.href = '#';

        // Bắt đầu với viền xám chung
        let aClasses = 'page-link border-secondary';

        if (isCurrent) {
            // Nút đang hoạt động: Nền xanh primary/cyan (từ CSS gốc), chữ tối
            aClasses += ' bg-primary text-dark fw-bold border-primary';
        } else {
            // Nút thường: Nền tối (bg-dark), chữ trắng (text-white)
            aClasses += ' bg-dark text-white';
        }

        a.className = aClasses;
        a.textContent = text;

        if (!isDisabled) {
            a.onclick = (e) => {
                e.preventDefault();
                loadSeatsListByAuditorium(pageNumber);
            };
        }
        li.appendChild(a);
        return li;
    };


    // Nút Previous («)
    ul.appendChild(createPageLink('«', currentPage - 1, currentPage === 1));

    // Hiển thị tối đa 5 trang (có thể điều chỉnh)
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageLink(i, i, false, i === currentPage));
    }

    // Nút Next (»)
    ul.appendChild(createPageLink('»', currentPage + 1, currentPage === totalPages));

    paginationControls.appendChild(ul);
}
// --- HÀM TẢI DANH SÁCH GHẾ THEO PHÒNG CHIẾU (loadSeatsListByAuditorium) ---
async function loadSeatsListByAuditorium(page = 1) {
    const auditoriumID = diagramAuditoriumSelect.value;
    seatsBody.innerHTML = '';
    paginationControls.innerHTML = ''; // Xóa phân trang cũ

    if (!auditoriumID) {
        seatsBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Vui lòng chọn Phòng Chiếu ở mục Sơ Đồ để tải danh sách ghế.</td></tr>';
        return;
    }

    try {
        const allFilteredSeats = allSeatsData
            .filter(s => s.auditoriumID === parseInt(auditoriumID))
            // Sắp xếp theo Dãy (Row) rồi đến Cột (Column) để dễ nhìn
            .sort((a, b) => {
                if (a.seatRow < b.seatRow) return -1;
                if (a.seatRow > b.seatRow) return 1;
                return a.columnNumber - b.columnNumber;
            });

        if (allFilteredSeats.length === 0) {
            seatsBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Phòng chiếu này chưa có ghế nào.</td></tr>';
            return;
        }

        // --- LOGIC PHÂN TRANG ---
        currentPage = page;
        const totalItems = allFilteredSeats.length;
        const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

        const start = (currentPage - 1) * ITEMS_PER_PAGE;
        const end = start + ITEMS_PER_PAGE;
        const seatsToDisplay = allFilteredSeats.slice(start, end);

        // Hiển thị phân trang
        renderPaginationControls(totalPages);
        // -----------------------

        seatsToDisplay.forEach(seat => {
            const row = seatsBody.insertRow();
            row.insertCell(0).textContent = seat.seatID;
            // ĐÃ SỬA: Chỉ hiển thị tên Phòng Chiếu
            row.insertCell(1).textContent = seat.auditoriumName;
            row.insertCell(2).textContent = `${seat.seatRow}/${seat.columnNumber}`;
            row.insertCell(3).textContent = seat.seatNumber;
            // ĐÃ SỬA: Loại bỏ ID loại ghế (chỉ còn Tên Loại)
            row.insertCell(4).textContent = seat.typeName;
            row.insertCell(5).textContent = seat.status;

            const actionsCell = row.insertCell(6);
            actionsCell.className = 'action-btns';

            const editBtn = document.createElement('button');
            editBtn.textContent = 'Sửa';
            editBtn.className = 'btn btn-warning btn-sm me-2';
            editBtn.onclick = () => populateFormForUpdate(seat);
            actionsCell.appendChild(editBtn);

            // *** ĐÃ LOẠI BỎ: NÚT XÓA CỨNG (DELETE) ***
        });

    } catch (error) {
        console.error('Error loading seats list:', error);
        seatsBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger" style="color:var(--red)">Lỗi khi tải danh sách ghế: ${error.message}</td></tr>`;
    }
}


// --- HÀM TẢI VÀ VẼ SƠ ĐỒ GHẾ (loadSeatDiagram) ---
async function loadSeatDiagram(auditoriumId) {
    if (!auditoriumId) {
        seatDiagram.innerHTML = '<p class="text-center" style="color:var(--muted)">Vui lòng chọn Phòng Chiếu.</p>';
        return;
    }

    seatDiagram.innerHTML = '<div class="screen">MÀN HÌNH CHIẾU</div><p class="text-center" style="color:var(--muted)">Đang tải sơ đồ...</p>';

    const seats = allSeatsData.filter(s => s.auditoriumID === parseInt(auditoriumId));

    if (seats.length === 0) {
        seatDiagram.innerHTML = '<div class="screen">MÀN HÌNH CHIẾU</div><p class="text-center" style="color:var(--muted)">Phòng chiếu này chưa có ghế nào.</p>';
        return;
    }

    const seatsByRow = seats.reduce((acc, seat) => {
        const row = seat.seatRow;
        if (!acc[row]) {
            acc[row] = [];
        }
        acc[row].push(seat);
        return acc;
    }, {});

    const sortedRows = Object.keys(seatsByRow).sort();

    let diagramHTML = '<div class="screen">MÀN HÌNH CHIẾU</div>';
    sortedRows.forEach(rowKey => {
        const rowSeats = seatsByRow[rowKey].sort((a, b) => a.columnNumber - b.columnNumber);

        diagramHTML += `<div class="seat-row mb-2 d-flex align-items-center" data-row="${rowKey}">`;
        diagramHTML += `<span class="seat-label me-3 fw-bold">${rowKey}</span>`;

        rowSeats.forEach(seat => {
            const colorClasses = getSeatClass(seat);
            // ĐÃ SỬA: Loại bỏ TypeID khỏi tooltip
            let typeTooltip = `${seat.typeName}`;

            let isCoupleSeat = seat.seatNumber.includes('-');
            let seatWidth = isCoupleSeat ? 'width: 65px; margin-right: 8px;' : 'width: 30px; margin-right: 4px;';

            diagramHTML += `
                <div class="seat-box ${colorClasses}"
                     data-id="${seat.seatID}"
                     data-seat-data='${JSON.stringify(seat)}'
                     title="${seat.seatNumber} | ${seat.status} | ${typeTooltip}"
                     style="${seatWidth}">
                    ${isCoupleSeat ? seat.seatNumber.split('-')[0] : seat.columnNumber}
                </div>
            `;
        });

        diagramHTML += '</div>';
    });

    seatDiagram.innerHTML = diagramHTML;

    document.querySelectorAll('.seat-box').forEach(box => {
        box.addEventListener('click', (e) => {
            document.querySelectorAll('.seat-box').forEach(b => b.classList.remove('seat-selected'));
            e.currentTarget.classList.add('seat-selected');

            const seatData = JSON.parse(e.currentTarget.getAttribute('data-seat-data'));
            populateFormForUpdate(seatData);
        });
    });
}


// --- HÀM TẢI CHÍNH (loadSeats) ---
async function loadSeats() {
    await loadForeignKeys();
    await loadAllSeatsData();
    // Bắt đầu tải trang 1
    loadSeatsListByAuditorium(1);

    if (diagramAuditoriumSelect.value) {
        loadSeatDiagram(diagramAuditoriumSelect.value);
    } else {
        seatDiagram.innerHTML = '<p class="text-center" style="color:var(--muted)">Vui lòng chọn Phòng Chiếu để hiển thị sơ đồ.</p>';
    }
}


// =========================================================================
// CÁC HÀM CRUD VÀ XỬ LÝ FORM
// =========================================================================

// --- HÀM ĐIỀN DỮ LIỆU VÀO FORM (KHI SỬA) ---
function populateFormForUpdate(seat) {
    bulkSeatForm.style.display = 'none';
    bulkUpdateForm.style.display = 'none';

    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập Nhật Ghế (ID: ${seat.seatID})`;
    submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i> Lưu Cập Nhật';
    cancelBtn.style.display = 'inline-block';

    // *** ĐÃ CHỈNH SỬA: ĐẢM BẢO NÚT XÓA CỨNG KHÔNG BAO GIỜ HIỂN THỊ ***
    if (deleteBtnOnForm) {
        deleteBtnOnForm.style.display = 'none';
    }
    // ------------------------------------

    seatIdField.value = seat.seatID;
    const currentAud = allAuditoriums.find(a => a.auditoriumID === seat.auditoriumID);
    if(currentAud && singleBranchSelect) {
        singleBranchSelect.value = currentAud.branchName;
        renderAuditoriumOptions(auditoriumSelect, currentAud.branchName);
    }

    auditoriumSelect.value = seat.auditoriumID;
    seatTypeSelect.value = seat.typeID;
    document.getElementById('seatNumber').value = seat.seatNumber;
    document.getElementById('seatRow').value = seat.seatRow;
    document.getElementById('columnNumber').value = seat.columnNumber;
    document.getElementById('status').value = seat.status;

    window.scrollTo({ top: 0, behavior: 'smooth' });
}


// --- HÀM RESET FORM ---
function resetForm() {
    seatForm.reset();
    seatIdField.value = '';

    formTitle.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Thêm Ghế Ngồi Mới (Đơn Lẻ)';
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Tạo Ghế Ngồi';
    cancelBtn.style.display = 'none';

    // *** ĐÃ CHỈNH SỬA: ĐẢM BẢO NÚT XÓA CỨNG KHÔNG BAO GIỜ HIỂN THỊ ***
    if (deleteBtnOnForm) {
        deleteBtnOnForm.style.display = 'none';
    }

    bulkSeatForm.style.display = 'block';
    bulkUpdateForm.style.display = 'block';

    document.querySelectorAll('.seat-box').forEach(b => b.classList.remove('seat-selected'));

    // Cập nhật lại options của các dropdown form CRUD đơn lẻ
    const currentBranch = singleBranchSelect.value;
    if(currentBranch) {
        renderAuditoriumOptions(auditoriumSelect, currentBranch);
    }
}

// --- HÀM TẠO/CẬP NHẬT (CRUD ĐƠN LẺ) ---
async function handleFormSubmission(e) {
    e.preventDefault();

    const id = seatIdField.value;
    const isUpdate = id !== '';
    const method = isUpdate ? 'PUT' : 'POST';
    const url = isUpdate ? `${SEAT_API_URL}/${id}` : SEAT_API_URL;

    const requestBody = {
        auditoriumID: parseInt(auditoriumSelect.value),
        typeID: parseInt(seatTypeSelect.value),
        seatNumber: document.getElementById('seatNumber').value.trim(),
        seatRow: document.getElementById('seatRow').value.trim(),
        columnNumber: parseInt(document.getElementById('columnNumber').value),
        status: document.getElementById('status').value
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
                // Xóa mềm được thực hiện khi cập nhật status thành Broken
                text: `Ghế ngồi đã được ${isUpdate ? 'cập nhật' : 'tạo'} thành công!`,
                icon: 'success',
                timer: 2000
            });

            resetForm();
            loadSeats();
        } else {
            const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
            Swal.fire('Thất bại', `Lỗi ${response.status}: ${errorData.message || errorData.error || 'Vui lòng kiểm tra dữ liệu đầu vào.'}`, 'error');
        }
    } catch (error) {
        console.error('Lỗi kết nối hoặc xử lý:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ.', 'error');
    }
}

// --- HÀM TẠO GHẾ HÀNG LOẠT (BULK CREATION) ---
async function handleBulkFormSubmission(e) {
    e.preventDefault();

    const requestBody = {
        auditoriumID: parseInt(bulkAuditoriumSelect.value),
        typeID: parseInt(bulkSeatTypeSelect.value),
        rowCount: parseInt(rowCountInput.value),
        columnCount: parseInt(columnCountInput.value),
        startRowChar: document.getElementById('startChar').value.toUpperCase().trim()
    };

    if (requestBody.rowCount * requestBody.columnCount === 0) {
        Swal.fire('Lỗi', 'Số lượng ghế không hợp lệ. Vui lòng kiểm tra Số Dãy và Số Cột.', 'error');
        return;
    }

    const result = await Swal.fire({
        title: 'Xác nhận Tạo Sơ Đồ?',
        text: `Bạn sẽ tạo ${requestBody.rowCount * requestBody.columnCount} ghế mới trong Phòng Chiếu này. Bạn có chắc chắn?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Đúng, TẠO!',
        cancelButtonText: 'Hủy',
        reverseButtons: true,
    });

    if (!result.isConfirmed) return;

    try {
        const response = await fetch(`${SEAT_API_URL}/bulk`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody),
        });

        if (response.ok || response.status === 201) {
            Swal.fire({
                title: 'Thành công!',
                text: `Đã tạo ${requestBody.rowCount * requestBody.columnCount} ghế thành công.`,
                icon: 'success',
                timer: 3000
            });

            bulkSeatForm.reset();
            totalSeatsSpan.textContent = '0';
            loadSeats();
        } else {
            const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
            Swal.fire('Thất bại', `Lỗi ${response.status}: ${errorData.message || errorData.error || 'Vui lòng kiểm tra dữ liệu đầu vào.'}`, 'error');
        }
    } catch (error) {
        console.error('Lỗi kết nối hoặc xử lý:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ khi tạo hàng loạt.', 'error');
    }
}


// --- HÀM CẬP NHẬT LOẠI/TRẠNG THÁI/GHẾ ĐÔI HÀNG LOẠT ---
async function handleBulkUpdateSubmission(e) {
    e.preventDefault();

    const newTypeIDValue = newTypeSelect.value;
    const newStatusValue = newStatusSelect.value;
    const isConvertCouple = isConvertCoupleSeatCheckbox.checked;
    const isSeparateCouple = isSeparateCoupleSeatCheckbox.checked;

    // --- KIỂM TRA LOGIC TÁCH/GỘP ---
    if (isConvertCouple && isSeparateCouple) {
        Swal.fire('Lỗi', 'Không thể vừa Gộp Ghế Đôi vừa Tách Ghế Đơn trong một lần cập nhật.', 'error');
        return;
    }

    if (!newTypeIDValue && !newStatusValue && !isConvertCouple && !isSeparateCouple) {
        Swal.fire('Lỗi', 'Vui lòng chọn Loại Ghế, Trạng Thái, Gộp Ghế hoặc Tách Ghế để cập nhật.', 'warning');
        return;
    }

    if (isConvertCouple && !newTypeIDValue) {
        Swal.fire('Lỗi', 'Nếu chọn Gộp Ghế Đôi, bạn PHẢI chọn một Loại Ghế Mới.', 'error');
        return;
    }

    // *** RÀNG BUỘC KHI TÁCH GHẾ ***
    if (isSeparateCouple && !newTypeIDValue) {
        Swal.fire('Lỗi', 'Nếu chọn Tách Ghế Đơn, bạn PHẢI chọn một Loại Ghế Mới (Thường hoặc VIP) cho các ghế đơn được tạo ra.', 'error');
        return;
    }
    // ------------------------------------------

    const requestBody = {
        auditoriumID: parseInt(updateAuditoriumSelect.value),
        newTypeID: newTypeIDValue ? parseInt(newTypeIDValue) : null,
        newStatus: newStatusValue || null,
        seatRowToUpdate: rowToUpdateInput.value.toUpperCase().trim(),
        isConvertCoupleSeat: isConvertCouple,
        isSeparateCoupleSeat: isSeparateCouple
    };

    let confirmationText = `Bạn sẽ cập nhật Dãy ${requestBody.seatRowToUpdate}.`;
    if (isConvertCouple) {
        confirmationText = `Bạn sẽ chuyển Dãy ${requestBody.seatRowToUpdate} thành Ghế Đôi/VIP (Gộp Ghế).`;
    } else if (isSeparateCouple) {
        confirmationText = `Bạn sẽ chuyển Dãy ${requestBody.seatRowToUpdate} thành Ghế Đơn (Tách Ghế). Thao tác này sẽ **tạo mới các ghế chẵn**.`;
    }

    const result = await Swal.fire({
        title: 'Xác nhận Cập Nhật Dãy Ghế?',
        text: confirmationText,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Đúng, CẬP NHẬT!',
        cancelButtonText: 'Hủy',
        reverseButtons: true,
    });

    if (!result.isConfirmed) return;

    try {
        const response = await fetch(`${SEAT_API_URL}/bulk-update-row`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody),
        });

        if (response.ok || response.status === 200) {
            Swal.fire({
                title: 'Thành công!',
                text: `Đã cập nhật/chuyển đổi dãy ghế ${requestBody.seatRowToUpdate} thành công.`,
                icon: 'success',
                timer: 3000
            });
            bulkUpdateForm.reset();
            loadSeats();
        } else {
            const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
            Swal.fire('Thất bại', `Lỗi ${response.status}: ${errorData.message || errorData.error || 'Vui lòng kiểm tra dữ liệu đầu vào.'}`, 'error');
        }
    } catch (error) {
        console.error('Lỗi kết nối hoặc xử lý:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ khi cập nhật hàng loạt.', 'error');
    }
}

// --- HÀM XÓA CỨNG (ĐÃ LOẠI BỎ THEO YÊU CẦU) ---
// Thay vì xóa cứng, người dùng sẽ cập nhật trạng thái ghế thành 'Broken' trong form sửa ghế đơn lẻ.

// --- HÀM TÍNH TỔNG GHẾ HÀNG LOẠT ---
function calculateTotalSeats() {
    const rows = parseInt(rowCountInput.value) || 0;
    const columns = parseInt(columnCountInput.value) || 0;
    totalSeatsSpan.textContent = (rows * columns).toLocaleString('en-US');
}

// --- GẮN SỰ KIỆN ---
seatForm.addEventListener('submit', handleFormSubmission);
bulkSeatForm.addEventListener('submit', handleBulkFormSubmission);
bulkUpdateForm.addEventListener('submit', handleBulkUpdateSubmission);

diagramAuditoriumSelect.addEventListener('change', (e) => {
    document.querySelectorAll('.seat-box').forEach(b => b.classList.remove('seat-selected'));
    resetForm();
    loadSeatDiagram(e.target.value);
    // Khi đổi Auditorium, reset về trang 1
    loadSeatsListByAuditorium(1);
});

// Nút Tải Lại sẽ tải lại trang 1
loadButton.addEventListener('click', () => loadSeatsListByAuditorium(1));
cancelBtn.addEventListener('click', resetForm);

rowCountInput.addEventListener('input', calculateTotalSeats);
columnCountInput.addEventListener('input', calculateTotalSeats);

// Tải danh sách khi trang được load lần đầu
document.addEventListener('DOMContentLoaded', loadSeats);