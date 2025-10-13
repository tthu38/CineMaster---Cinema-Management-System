import { requireAuth } from "./api/config.js";
import { screeningPeriodApi } from './api/screeningPeriodApi.js';
import { branchApi } from "./api/branchApi.js";

requireAuth();

const periodsBody = document.getElementById("periods-body");
const filterBranchSelect = document.getElementById("filterBranchID");
const loadPeriodsBtn = document.getElementById("load-periods");
const paginationFooter = document.getElementById("pagination-footer");

let allBranches = [];
let allMovies = [];
let allPeriodsData = [];

const PAGE_SIZE = 10;
let currentPage = 0;
const TABLE_COLSPAN = 6;

// HÀM TẢI KHÓA NGOẠI (BRANCH, MOVIE)
async function loadForeignKeys() {
    try {
        // --- 1️⃣ Lấy danh sách Chi nhánh ---
        const branches = await branchApi.getAllBranches();
        allBranches = Array.isArray(branches)
            ? branches.filter((b) => b.isActive)
            : [];

        // Cập nhật danh sách lọc chi nhánh
        filterBranchSelect.innerHTML =
            '<option value="all" selected>Lọc theo Chi nhánh...</option>';
        allBranches.forEach((branch) => {
            const option = new Option(branch.branchName, branch.branchId);
            filterBranchSelect.appendChild(option);
        });

        // --- 2️⃣ Lấy danh sách phim (nếu cần hiển thị poster) ---
        // Nếu bạn có movieApi thì import nó ở api.js, ví dụ:
        // import { movieApi } from '../js/api.js';
        // Ở đây tạm thời để trống do bạn chưa kết nối MovieController
        allMovies = []; // placeholder

    } catch (error) {
        console.error("❌ Lỗi khi tải khóa ngoại:", error);
        Swal.fire(
            "Cảnh báo",
            "Không thể tải đầy đủ danh sách Phim hoặc Chi nhánh.",
            "warning"
        );
    }
}

// HÀM LOAD DANH SÁCH KHOẢNG CHIẾU (READ + PHÂN TRANG)
async function loadPeriods(loadFromApi = false, page = 0) {
    try {
        currentPage = page;
        const branchId = filterBranchSelect.value;

        periodsBody.innerHTML = `<tr>
            <td colspan="${TABLE_COLSPAN}" class="text-center text-muted">
                <i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải dữ liệu...
            </td>
        </tr>`;

        // --- 1️⃣ Lấy dữ liệu từ API ---
        if (loadFromApi) {
            if (branchId !== "all") {
                allPeriodsData = await screeningPeriodApi.getByBranch(branchId);
            } else {
                allPeriodsData = await screeningPeriodApi.getAll();
            }
            currentPage = 0; // reset về trang đầu
        }

        // --- 2️⃣ Lọc dữ liệu đang hoạt động ---
        const activePeriods = allPeriodsData.filter(
            (p) => p.isActive === true || p.isActive === 1 || p.isActive === undefined
        );

        // --- 3️⃣ Phân trang ---
        const totalItems = activePeriods.length;
        const totalPages = Math.ceil(totalItems / PAGE_SIZE);
        const start = currentPage * PAGE_SIZE;
        const end = start + PAGE_SIZE;
        const pageItems = activePeriods.slice(start, end);

        // --- 4️⃣ Hiển thị dữ liệu ---
        if (pageItems.length === 0) {
            periodsBody.innerHTML = `<tr>
                <td colspan="${TABLE_COLSPAN}" class="text-center text-muted">
                    Không có khoảng chiếu nào đang hoạt động.
                </td>
            </tr>`;
            paginationFooter.innerHTML = "";
            return;
        }

        periodsBody.innerHTML = "";
        for (const period of pageItems) {
            const branch =
                allBranches.find(
                    (b) =>
                        String(b.branchId || b.id) ===
                        String(period.branchId)
                ) || {};

            const movieTitle = period.movieTitle || `Phim ID: ${period.movieId}`;
            const posterUrl =
                period.posterUrl ||
                "../images/default-poster.png";

            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${period.id}</td>
                <td><img src="${posterUrl}" alt="${movieTitle}"
                    class="movie-poster-img"
                    onerror="this.onerror=null;this.src='../images/default-poster.png';">
                </td>
                <td>${movieTitle}</td>
                <td>${branch.branchName || "N/A"}</td>
                <td>${new Date(period.startDate).toLocaleDateString("vi-VN")}</td>
                <td>${new Date(period.endDate).toLocaleDateString("vi-VN")}</td>
            `;
            periodsBody.appendChild(row);
        }

        // --- 5️⃣ Tạo phân trang ---
        renderPagination(totalPages, currentPage);
    } catch (error) {
        console.error("❌ Lỗi khi tải khoảng chiếu:", error);
        periodsBody.innerHTML = `<tr>
            <td colspan="${TABLE_COLSPAN}" class="text-center text-danger">
                Lỗi khi tải dữ liệu: ${error.message}
            </td>
        </tr>`;
    }
}

// HÀM PHÂN TRANG
function renderPagination(totalPages, currentPage) {
    paginationFooter.innerHTML = "";

    // Previous
    paginationFooter.innerHTML += `
        <li class="page-item ${currentPage === 0 ? "disabled" : ""}">
            <a class="page-link" href="#" onclick="loadPeriods(false, ${
        currentPage - 1
    })">Trước</a>
        </li>
    `;

    for (let i = 0; i < totalPages; i++) {
        paginationFooter.innerHTML += `
            <li class="page-item ${i === currentPage ? "active" : ""}">
                <a class="page-link" href="#" onclick="loadPeriods(false, ${i})">${
            i + 1
        }</a>
            </li>
        `;
    }

    // Next
    paginationFooter.innerHTML += `
        <li class="page-item ${
        currentPage === totalPages - 1 ? "disabled" : ""
    }">
            <a class="page-link" href="#" onclick="loadPeriods(false, ${
        currentPage + 1
    })">Sau</a>
        </li>
    `;
}

// GẮN SỰ KIỆN & KHỞI TẠO
if (filterBranchSelect) {
    filterBranchSelect.addEventListener("change", () => loadPeriods(true));
}
if (loadPeriodsBtn) {
    loadPeriodsBtn.addEventListener("click", () => loadPeriods(true));
}

document.addEventListener("DOMContentLoaded", async () => {
    await loadForeignKeys();
    await loadPeriods(true);
});
