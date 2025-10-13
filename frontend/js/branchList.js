import { branchApi } from "./api/branchApi.js";

// --- BIẾN PHÂN TRANG ---
let allBranchesData = [];
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

const branchesBody = document.getElementById("branches-body");
const loadButton = document.getElementById("load-branches");
const paginationControls = document.getElementById("pagination-controls");

// --- HÀM PHÂN TRANG ---
function renderPaginationControls(totalPages) {
    paginationControls.innerHTML = "";
    if (totalPages <= 1) return;

    const ul = document.createElement("ul");
    ul.className = "pagination pagination-sm";

    const createPageLink = (text, pageNumber, isDisabled = false, isCurrent = false) => {
        const li = document.createElement("li");
        li.className = `page-item ${isDisabled ? "disabled" : ""} ${isCurrent ? "active" : ""}`;
        const a = document.createElement("a");
        a.href = "#";
        a.className = "page-link";
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

    ul.appendChild(createPageLink("«", currentPage - 1, currentPage === 1));

    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageLink(i, i, false, i === currentPage));
    }

    ul.appendChild(createPageLink("»", currentPage + 1, currentPage === totalPages));
    paginationControls.appendChild(ul);
}

// --- HIỂN THỊ DANH SÁCH ---
function displayBranches(page = 1) {
    branchesBody.innerHTML = "";
    paginationControls.innerHTML = "";

    if (!allBranchesData || allBranchesData.length === 0) {
        branchesBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center" style="color:var(--muted)">
                    Chưa có chi nhánh nào đang hoạt động.
                </td>
            </tr>`;
        return;
    }

    currentPage = page;
    const totalItems = allBranchesData.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const branchesToDisplay = allBranchesData.slice(start, end);

    renderPaginationControls(totalPages);

    branchesToDisplay.forEach((branch) => {
        const row = branchesBody.insertRow();
        row.insertCell(0).textContent = branch.branchId ?? "-";
        row.insertCell(1).textContent = branch.branchName ?? "-";
        row.insertCell(2).textContent = branch.address ?? "-";
        row.insertCell(3).textContent = branch.phone ?? "-";
        row.insertCell(4).textContent = branch.email ?? "-";
        row.insertCell(5).textContent = `${branch.openTime ?? "??"} - ${branch.closeTime ?? "??"}`;
        const managerDisplay = branch.managerId
            ? `${branch.managerId} (${branch.managerName || "N/A"})`
            : "Chưa gán";
        row.insertCell(6).textContent = managerDisplay;
    });
}

// --- GỌI API TỪ api.js ---
async function loadBranches() {
    branchesBody.innerHTML = `
        <tr><td colspan="7" class="text-center" style="color:var(--muted)">
        Đang tải danh sách...</td></tr>`;
    paginationControls.innerHTML = "";

    try {
        const res = await branchApi.getAllActive(); // ✅ Gọi API từ api.js
        if (!res) throw new Error("Không thể kết nối tới server");

        allBranchesData = Array.isArray(res) ? res : res.result || [];
        displayBranches(1);
    } catch (err) {
        console.error("Lỗi khi tải danh sách chi nhánh:", err);
        Swal.fire("Lỗi Kết Nối", `Không thể kết nối đến máy chủ: ${err.message}`, "error");
        branchesBody.innerHTML = `
            <tr><td colspan="7" class="text-center" style="color:var(--red)">
            Lỗi kết nối: ${err.message}</td></tr>`;
    }
}

// --- SỰ KIỆN ---
loadButton.addEventListener("click", loadBranches);
document.addEventListener("DOMContentLoaded", loadBranches);
