// =============================================================
// 📂 period-crud.js — Quản lý Khoảng Thời Gian Chiếu (Admin/Manager)
// =============================================================

import { requireAuth, branchApi, movieApi, screeningPeriodApi } from "./api.js";
import Swal from "https://cdn.jsdelivr.net/npm/sweetalert2@11/+esm";

requireAuth();

// --- DOM elements ---
const periodForm = document.getElementById("period-form");
const periodsBody = document.getElementById("periods-body");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelBtn = document.getElementById("cancel-btn");
const periodID = document.getElementById("periodID");

const movieSelect = document.getElementById("movieID");
const branchCheckboxContainer = document.getElementById("branchCheckboxContainer");
const filterBranchSelect = document.getElementById("filterBranchID");
const startDateInput = document.getElementById("startDate");
const endDateInput = document.getElementById("endDate");

const branchHint = document.getElementById("branch-hint");
const posterPreviewContainer = document.getElementById("movie-poster-preview");
const posterPreviewImg = document.getElementById("poster-img-preview");
const paginationFooter = document.getElementById("pagination-footer");

// --- Data state ---
let allMovies = [];
let allBranches = [];
let allPeriodsData = [];
let currentPage = 0;
const PAGE_SIZE = 10;
const TABLE_COLSPAN = 8;

// =============================================================
// 🧩 Load danh sách phim & chi nhánh
// =============================================================
async function loadForeignKeys() {
    branchCheckboxContainer.innerHTML = `<i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải chi nhánh...`;
    movieSelect.innerHTML = `<option value="" disabled selected hidden>Đang tải phim...</option>`;

    try {
        // --- 1️⃣ Load chi nhánh ---
        const branches = await branchApi.getAllBranches();
        allBranches = Array.isArray(branches) ? branches : [];

        filterBranchSelect.innerHTML = `<option value="all" selected>Lọc theo Chi nhánh...</option>`;
        branchCheckboxContainer.innerHTML = "";

        allBranches.forEach(b => {
            const branchId = b.branchId || b.id;
            const isActive = b.isActive === true || b.isActive === 1;
            const status = isActive ? "" : " (Đã đóng)";
            const checkboxDiv = document.createElement("div");
            checkboxDiv.className = "form-check";
            checkboxDiv.innerHTML = `
                <input class="form-check-input branch-checkbox" type="checkbox" value="${branchId}" id="branch-${branchId}">
                <label class="form-check-label" for="branch-${branchId}">
                    ${b.branchName}${status}
                </label>
            `;
            branchCheckboxContainer.appendChild(checkboxDiv);
            filterBranchSelect.appendChild(new Option(`${b.branchName}${status}`, branchId));
        });

        // --- 2️⃣ Load danh sách phim ---
        const movies = await movieApi.getAll();
        allMovies = Array.isArray(movies) ? movies : movies.result || [];

        movieSelect.innerHTML = `<option value="" selected hidden>--- Chọn phim ---</option>`;
        allMovies.forEach(m => {
            const id = m.movieID || m.id;
            movieSelect.appendChild(new Option(m.title, id));
        });
    } catch (err) {
        console.error("Lỗi tải dữ liệu:", err);
        Swal.fire("Lỗi", "Không thể tải danh sách phim hoặc chi nhánh.", "error");
    }
}

// =============================================================
// 🎬 Hiển thị danh sách khoảng thời gian chiếu
// =============================================================
async function loadPeriods(page = 0, reload = false) {
    currentPage = page;
    const branchId = filterBranchSelect.value;
    periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2"></i> Đang tải dữ liệu...</td></tr>`;

    try {
        if (reload || allPeriodsData.length === 0) {
            allPeriodsData = branchId === "all"
                ? await screeningPeriodApi.getAll()
                : await screeningPeriodApi.getByBranch(branchId);
        }

        const totalPages = Math.ceil(allPeriodsData.length / PAGE_SIZE);
        const start = page * PAGE_SIZE;
        const end = start + PAGE_SIZE;
        const displayData = allPeriodsData.slice(start, end);

        periodsBody.innerHTML = "";

        if (displayData.length === 0) {
            periodsBody.innerHTML = `<tr><td colspan="${TABLE_COLSPAN}" class="text-center text-muted">Không tìm thấy dữ liệu.</td></tr>`;
            paginationFooter.innerHTML = "";
            return;
        }

        displayData.forEach(p => {
            const movie = allMovies.find(m => String(m.movieID || m.id) === String(p.movieId));
            const branch = allBranches.find(b => String(b.branchId || b.id) === String(p.branchId));

            const movieTitle = movie ? movie.title : `Phim ID ${p.movieId}`;
            const posterUrl = movie?.posterUrl || "../images/default-poster.png";
            const branchName = branch ? branch.branchName : `Chi nhánh #${p.branchId}`;
            const isActive = p.isActive === true || p.isActive === 1;

            const row = periodsBody.insertRow();
            row.insertCell(0).textContent = p.id;
            row.insertCell(1).innerHTML = `<img src="${posterUrl}" class="movie-poster-img">`;
            row.insertCell(2).textContent = movieTitle;
            row.insertCell(3).textContent = branchName;
            row.insertCell(4).textContent = new Date(p.startDate).toLocaleDateString("vi-VN");
            row.insertCell(5).textContent = new Date(p.endDate).toLocaleDateString("vi-VN");
            row.insertCell(6).innerHTML = `<span class="badge ${isActive ? "bg-success" : "bg-danger"}">${isActive ? "Đang hoạt động" : "Đã tắt"}</span>`;
            row.insertCell(7).innerHTML = `
                <button class="btn btn-warning btn-sm me-2" onclick="editPeriod(${p.id})"><i class="fa-solid fa-pen-to-square"></i></button>
                <button class="btn ${isActive ? "btn-danger" : "btn-success"} btn-sm" onclick="togglePeriodStatus(${p.id}, ${isActive})">
                    <i class="fa-solid fa-${isActive ? "power-off" : "check"}"></i>
                </button>
            `;
        });

        renderPagination(totalPages);
    } catch (err) {
        console.error("Lỗi loadPeriods:", err);
        Swal.fire("Lỗi", "Không thể tải dữ liệu khoảng chiếu.", "error");
    }
}

// =============================================================
// 📅 Phân trang
// =============================================================
function renderPagination(totalPages) {
    paginationFooter.innerHTML = "";

    const createBtn = (label, page, disabled = false, active = false) => `
        <li class="page-item ${disabled ? "disabled" : ""} ${active ? "active" : ""}">
            <a class="page-link" href="#" onclick="loadPeriods(${page}); return false;">${label}</a>
        </li>
    `;

    paginationFooter.innerHTML += createBtn("«", currentPage - 1, currentPage === 0);
    for (let i = 0; i < totalPages; i++) {
        paginationFooter.innerHTML += createBtn(i + 1, i, false, i === currentPage);
    }
    paginationFooter.innerHTML += createBtn("»", currentPage + 1, currentPage >= totalPages - 1);
}

// =============================================================
// 🖼️ Xem trước poster khi chọn phim
// =============================================================
movieSelect.addEventListener("change", () => {
    const movie = allMovies.find(m => String(m.movieID || m.id) === String(movieSelect.value));
    if (movie?.posterUrl) {
        posterPreviewImg.src = movie.posterUrl;
        posterPreviewContainer.style.display = "block";
    } else {
        posterPreviewContainer.style.display = "none";
    }
});

// =============================================================
// 🧾 Xử lý submit form (Tạo mới / Cập nhật)
// =============================================================
periodForm.addEventListener("submit", async e => {
    e.preventDefault();

    const id = periodID.value;
    const isUpdate = id !== "";
    const selectedBranches = Array.from(document.querySelectorAll(".branch-checkbox:checked")).map(cb => parseInt(cb.value));

    if (!movieSelect.value || selectedBranches.length === 0) {
        Swal.fire("Thiếu dữ liệu", "Vui lòng chọn phim và ít nhất một chi nhánh.", "warning");
        return;
    }

    const body = {
        movieId: parseInt(movieSelect.value),
        startDate: startDateInput.value,
        endDate: endDateInput.value
    };

    try {
        if (isUpdate) {
            if (selectedBranches.length !== 1) {
                Swal.fire("Lỗi", "Khi cập nhật chỉ chọn 1 chi nhánh.", "warning");
                return;
            }
            await screeningPeriodApi.update(id, { ...body, branchId: selectedBranches[0] });
            Swal.fire("Thành công", "Cập nhật khoảng chiếu thành công!", "success");
        } else {
            for (const branchId of selectedBranches) {
                await screeningPeriodApi.create({ ...body, branchId });
            }
            Swal.fire("Hoàn tất", `Tạo thành công ${selectedBranches.length} khoảng chiếu.`, "success");
        }

        resetForm();
        loadPeriods(0, true);
    } catch (err) {
        console.error("Lỗi xử lý form:", err);
        Swal.fire("Thất bại", err.message || "Không thể gửi yêu cầu.", "error");
    }
});

// =============================================================
// ✏️ Chỉnh sửa, reset và toggle trạng thái
// =============================================================
window.editPeriod = function (id) {
    const p = allPeriodsData.find(x => x.id === id);
    if (!p) return Swal.fire("Lỗi", "Không tìm thấy dữ liệu.", "error");

    periodID.value = p.id;
    movieSelect.value = p.movieId;
    startDateInput.value = p.startDate.slice(0, 10);
    endDateInput.value = p.endDate.slice(0, 10);

    document.querySelectorAll(".branch-checkbox").forEach(cb => {
        cb.disabled = true;
        cb.checked = String(cb.value) === String(p.branchId);
    });
    branchHint.style.display = "none";

    submitBtn.innerHTML = `<i class="fa-solid fa-floppy-disk me-2"></i>Cập nhật`;
    submitBtn.classList.replace("btn-primary", "btn-warning");
    cancelBtn.style.display = "inline-block";
    formTitle.innerHTML = `<i class="fa-solid fa-pen-to-square me-3"></i>Chỉnh sửa Khoảng Chiếu #${p.id}`;
};

function resetForm() {
    periodForm.reset();
    periodID.value = "";
    branchHint.style.display = "inline-block";
    document.querySelectorAll(".branch-checkbox").forEach(cb => (cb.disabled = false, cb.checked = false));
    submitBtn.innerHTML = `<i class="fa-solid fa-plus me-2"></i>Tạo Khoảng Chiếu`;
    submitBtn.classList.replace("btn-warning", "btn-primary");
    cancelBtn.style.display = "none";
    formTitle.innerHTML = `<i class="fa-solid fa-calendar-plus me-3"></i>Thêm Khoảng Thời Gian Chiếu`;
    posterPreviewContainer.style.display = "none";
}
cancelBtn.addEventListener("click", resetForm);

// =============================================================
// 🔄 Toggle trạng thái (Kích hoạt / Vô hiệu hóa)
// =============================================================
window.togglePeriodStatus = async function (id, isActive) {
    const action = isActive ? "vô hiệu hóa" : "kích hoạt lại";
    const result = await Swal.fire({
        title: `Xác nhận ${action}?`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Xác nhận",
        cancelButtonText: "Hủy"
    });

    if (!result.isConfirmed) return;

    try {
        if (isActive) await screeningPeriodApi.delete(id);
        else {
            const period = allPeriodsData.find(p => p.id === id);
            await screeningPeriodApi.update(id, { ...period, isActive: true });
        }

        Swal.fire("Thành công", `Đã ${action} khoảng chiếu #${id}`, "success");
        loadPeriods(currentPage, true);
    } catch (err) {
        Swal.fire("Lỗi", err.message || "Không thể thay đổi trạng thái.", "error");
    }
};

// =============================================================
// 🚀 Khởi tạo
// =============================================================
document.addEventListener("DOMContentLoaded", async () => {
    await loadForeignKeys();
    await loadPeriods(0, true);
    startDateInput.valueAsDate = new Date();
});
