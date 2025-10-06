// package com.example.cinemaster.js/branch-list-api.js

const API_BASE_URL = 'http://localhost:8080';
// Sửa đổi API_URL để trỏ đến endpoint chỉ lấy chi nhánh đang hoạt động
// Endpoint này dành cho Client/Customer
const API_URL = API_BASE_URL + '/api/v1/branches/active';

// --- BIẾN PHÂN TRANG (PAGINATION) ---
let allBranchesData = []; // Biến mới để lưu trữ toàn bộ dữ liệu
let currentPage = 1;
const ITEMS_PER_PAGE = 10; // Số chi nhánh tối đa hiển thị trên mỗi trang

const branchesBody = document.getElementById('branches-body');
const loadButton = document.getElementById('load-branches');
const paginationControls = document.getElementById('pagination-controls');

// --- HÀM TẠO CÁC NÚT PHÂN TRANG (Giữ nguyên) ---
function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = '';
    if (totalPages <= 1) return;

    // Sử dụng class Bootstrap pagination
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


// --- HÀM HIỂN THỊ DỮ LIỆU ĐÃ PHÂN TRANG (Giữ nguyên) ---
function displayBranches(page = 1) {
    branchesBody.innerHTML = '';
    paginationControls.innerHTML = '';

    if (allBranchesData.length === 0) {
        branchesBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Chưa có chi nhánh nào đang hoạt động.</td></tr>';
        return;
    }

    // --- LOGIC PHÂN TRANG ---
    currentPage = page;
    const totalItems = allBranchesData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const branchesToDisplay = allBranchesData.slice(start, end);

    // Hiển thị phân trang
    renderPaginationControls(totalPages);
    // -----------------------

    branchesToDisplay.forEach(branch => {
        const row = branchesBody.insertRow();
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
    });
}


// --- HÀM TẢI DỮ LIỆU GỐC (READ ALL - GET) (Giữ nguyên) ---
async function loadBranches() {
    branchesBody.innerHTML = '<tr><td colspan="7" class="text-center" style="color:var(--muted)">Đang tải danh sách...</td></tr>';
    paginationControls.innerHTML = '';
    try {
        const response = await fetch(API_URL); // API_URL mới đã được thay đổi
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        allBranchesData = await response.json();
        displayBranches(1);
    } catch (error) {
        console.error('Lỗi khi tải danh sách chi nhánh:', error);
        Swal.fire('Lỗi Kết Nối', `Lỗi kết nối đến máy chủ: ${error.message}`, 'error');
        branchesBody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:var(--red)">Lỗi kết nối: ${error.message}</td></tr>`;
    }
}

// --- GẮN SỰ KIỆN (Giữ nguyên) ---
loadButton.addEventListener('click', () => loadBranches());
document.addEventListener('DOMContentLoaded', loadBranches);