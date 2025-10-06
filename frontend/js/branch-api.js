// package com.example.cinemaster.js/branch-api.js

const API_BASE_URL = 'http://localhost:8080';
// ✨ SỬA ĐỔI 1: Endpoint dành cho Admin (Lấy TẤT CẢ Branch)
const API_URL = API_BASE_URL + '/api/v1/branches';

// --- BIẾN PHÂN TRANG (PAGINATION) ---
let allBranchesData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

const branchForm = document.getElementById('branch-form');
const branchesBody = document.getElementById('branches-body');
const loadButton = document.getElementById('load-branches');
const formTitle = document.getElementById('form-title');
const submitBtn = document.getElementById('submit-btn');
const cancelBtn = document.getElementById('cancel-btn');
const branchIdField = document.getElementById('branchId');
const paginationControls = document.getElementById('pagination-controls');


// --- HÀM TẠO CÁC NÚT PHÂN TRANG (GIỮ NGUYÊN) ---
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
        a.textContent = text;

        if (!isDisabled) {
            a.onclick = (e) => {
                e.preventDefault();
                displayBranches(pageNumber);
            };
        }
        li.appendChild(a);
        return li;
    };

    // Nút Previous
    ul.appendChild(createPageLink('«', currentPage - 1, currentPage === 1));

    // Hiển thị tối đa 5 trang
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageLink(i, i, false, i === currentPage));
    }

    // Nút Next
    ul.appendChild(createPageLink('»', currentPage + 1, currentPage === totalPages));

    paginationControls.appendChild(ul);
}


// --- HÀM HIỂN THỊ DỮ LIỆU (ĐÃ SỬA ĐỂ XỬ LÝ ISACTIVE) ---
function displayBranches(page = 1) {
    branchesBody.innerHTML = '';
    paginationControls.innerHTML = '';

    if (allBranchesData.length === 0) {
        // Cập nhật colspan lên 9 (8 cột dữ liệu + 1 cột Status)
        branchesBody.innerHTML = '<tr><td colspan="9" class="text-center" style="color:var(--muted)">Chưa có chi nhánh nào được tạo.</td></tr>';
        return;
    }

    // --- LOGIC PHÂN TRANG (GIỮ NGUYÊN) ---
    currentPage = page;
    const totalItems = allBranchesData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const branchesToDisplay = allBranchesData.slice(start, end);

    renderPaginationControls(totalPages);
    // -----------------------

    branchesToDisplay.forEach(branch => {
        const row = branchesBody.insertRow();

        // 8 cột dữ liệu
        row.insertCell(0).textContent = branch.branchId;
        row.insertCell(1).textContent = branch.branchName;
        row.insertCell(2).textContent = branch.address;
        row.insertCell(3).textContent = branch.phone;
        row.insertCell(4).textContent = branch.email;
        row.insertCell(5).textContent = `${branch.openTime} - ${branch.closeTime}`;
        const managerDisplay = branch.managerId
            ? `${branch.managerId} (${branch.managerName || 'N/A'})`
            : 'Chưa gán';
        row.insertCell(6).textContent = managerDisplay;

        // ✨ CỘT MỚI: TRẠNG THÁI (STATUS) ✨
        const statusCell = row.insertCell(7);
        const isActive = branch.isActive;
        statusCell.innerHTML = `<span class="badge bg-${isActive ? 'success' : 'danger'}">${isActive ? 'Hoạt động' : 'Đã đóng'}</span>`;

        // ✨ CỘT HÀNH ĐỘNG (ACTIONS) ✨
        const actionsCell = row.insertCell(8);

        // Nút SỬA
        const editBtn = document.createElement('button');
        editBtn.textContent = 'Sửa';
        editBtn.className = 'btn btn-warning btn-sm edit-btn me-2 mb-1';
        editBtn.onclick = () => populateFormForUpdate(branch);
        actionsCell.appendChild(editBtn);

        // Nút THAY ĐỔI TRẠNG THÁI
        const statusBtn = document.createElement('button');
        statusBtn.textContent = isActive ? 'Đóng' : 'Mở lại';
        statusBtn.className = `btn btn-sm ${isActive ? 'btn-secondary delete-btn' : 'btn-primary'}`;
        // Gọi hàm xử lý trạng thái mới
        statusBtn.onclick = () => handleStatusChange(branch.branchId, isActive);
        actionsCell.appendChild(statusBtn);
    });
}


// --- HÀM TẢI DỮ LIỆU GỐC (GIỮ NGUYÊN LOGIC FETCH) ---
async function loadBranches() {
    branchesBody.innerHTML = '<tr><td colspan="9" class="text-center" style="color:var(--muted)">Đang tải danh sách...</td></tr>';
    paginationControls.innerHTML = '';
    try {
        const response = await fetch(API_URL); // API_URL đã là endpoint ALL
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        allBranchesData = await response.json();
        displayBranches(1);
    } catch (error) {
        console.error('Lỗi khi tải danh sách chi nhánh:', error);
        Swal.fire('Lỗi Kết Nối', `Lỗi kết nối đến máy chủ: ${error.message}`, 'error');
        branchesBody.innerHTML = `<tr><td colspan="9" class="text-center" style="color:var(--red)">Lỗi kết nối: ${error.message}</td></tr>`;
    }
}


// --- HÀM XỬ LÝ THAY ĐỔI TRẠNG THÁI (Thay thế hàm deleteBranch) ---
async function handleStatusChange(id, currentIsActive) {
    const action = currentIsActive ? 'vô hiệu hóa (đóng)' : 'kích hoạt lại (mở)';
    const verb = currentIsActive ? 'DELETE' : 'POST';
    const url = currentIsActive ? `${API_URL}/${id}` : `${API_URL}/${id}/activate`;
    const title = currentIsActive ? 'Đóng Chi Nhánh?' : 'Mở Lại Chi Nhánh?';

    const result = await Swal.fire({
        title: title,
        html: `Bạn có chắc chắn muốn **${action}** chi nhánh ID: <b>${id}</b>?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: `Đúng, ${action}!`,
        cancelButtonText: 'Hủy bỏ',
        reverseButtons: true,
        customClass: {
            confirmButton: `btn ${currentIsActive ? 'btn-danger' : 'btn-primary'} me-3`,
            cancelButton: 'btn btn-secondary'
        },
        buttonsStyling: false
    });

    if (!result.isConfirmed) return;

    try {
        const response = await fetch(url, {
            method: verb,
        });

        if (response.status === 204 || response.ok) {
            Swal.fire({
                title: 'Thành Công!',
                text: `Chi nhánh ID ${id} đã được ${action} thành công.`,
                icon: 'success',
                timer: 2000
            });
            // Tải lại dữ liệu sau khi thay đổi trạng thái
            await loadBranches();
        } else {
            const errorText = await response.text();
            Swal.fire('Lỗi', `Lỗi ${response.status}: ${errorText || 'Không xác định'}`, 'error');
        }
    } catch (error) {
        console.error('Lỗi khi thay đổi trạng thái:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ.', 'error');
    }
}

// --- CÁC HÀM KHÁC (GIỮ NGUYÊN) ---

// --- HÀM TẠO HOẶC CẬP NHẬT (POST / PUT) ---
async function handleFormSubmission(e) {
    e.preventDefault();

    const id = branchIdField.value;
    const isUpdate = id !== '';
    const method = isUpdate ? 'PUT' : 'POST';
    const url = isUpdate ? `${API_URL}/${id}` : API_URL;

    const requestBody = {
        branchName: document.getElementById('branchName').value,
        address: document.getElementById('address').value,
        phone: document.getElementById('phone').value,
        email: document.getElementById('email').value,
        openTime: document.getElementById('openTime').value,
        closeTime: document.getElementById('closeTime').value,
        managerId: document.getElementById('managerId').value ? parseInt(document.getElementById('managerId').value) : null
    };

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
        });

        if (response.ok || response.status === 201) {
            Swal.fire({
                title: 'Thành công!',
                text: `Chi nhánh đã được ${isUpdate ? 'cập nhật' : 'tạo'} thành công!`,
                icon: 'success',
                timer: 2000
            });

            resetForm();
            loadBranches();
        } else {
            const errorData = await response.json().catch(() => ({ message: response.statusText || 'Lỗi không xác định' }));
            console.error(errorData);
            Swal.fire('Thất bại', `Lỗi khi ${isUpdate ? 'cập nhật' : 'tạo'} chi nhánh: ${errorData.message || errorData.error}`, 'error');
        }
    } catch (error) {
        console.error('Lỗi kết nối hoặc xử lý:', error);
        Swal.fire('Lỗi Kết Nối', 'Lỗi kết nối đến máy chủ.', 'error');
    }
}


// --- HÀM ĐIỀN DỮ LIỆU VÀO FORM (CHO THAO TÁC SỬA) ---
function populateFormForUpdate(branch) {
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-2"></i> Cập Nhật Chi Nhánh (ID: ${branch.branchId})`;
    submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i> Lưu Cập Nhật';
    cancelBtn.style.display = 'inline-block';

    branchIdField.value = branch.branchId;
    document.getElementById('branchName').value = branch.branchName;
    document.getElementById('address').value = branch.address;
    document.getElementById('phone').value = branch.phone;
    document.getElementById('email').value = branch.email;
    document.getElementById('openTime').value = branch.openTime;
    document.getElementById('closeTime').value = branch.closeTime;
    document.getElementById('managerId').value = branch.managerId || '';

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// --- HÀM ĐẶT LẠI FORM (SAU KHI THAO TÁC HOẶC HỦY) ---
function resetForm() {
    branchForm.reset();
    branchIdField.value = '';
    formTitle.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Thêm Chi Nhánh Mới';
    submitBtn.innerHTML = '<i class="fa-solid fa-plus me-2"></i> Tạo Chi Nhánh';
    cancelBtn.style.display = 'none';
}

// --- GẮN SỰ KIỆN ---
branchForm.addEventListener('submit', handleFormSubmission);
loadButton.addEventListener('click', loadBranches);
cancelBtn.addEventListener('click', resetForm);

// Tải danh sách khi trang được load lần đầu
document.addEventListener('DOMContentLoaded', loadBranches);