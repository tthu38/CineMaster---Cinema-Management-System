import { comboApi, branchApi, requireAuth } from "./api.js";

let dataTable;
let allCombos = [];
let currentBranch = "";
let currentAvailable = "";
let currentKeyword = "";

// ===== Init =====
async function init() {
    if (!requireAuth()) return;

    await loadBranches();
    await loadCombos();

    // 🎯 Lọc theo chi nhánh
    document.getElementById("branchFilter").addEventListener("change", handleFilters);

    // 🎯 Lọc theo trạng thái (có sẵn / hết hàng)
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentAvailable = btn.dataset.available; // "", "1", "0"
            handleFilters();
        });
    });

    // 🎯 Tìm kiếm realtime
    const searchInput = document.getElementById("searchInput");
    searchInput.addEventListener("input", () => {
        currentKeyword = searchInput.value.trim().toLowerCase();
        handleFilters();
    });
}

// ===== Load danh sách chi nhánh =====
async function loadBranches() {
    try {
        const data = await branchApi.getAll();
        if (!data) return;

        const branchSelect = document.getElementById("branchFilter");
        branchSelect.innerHTML = `<option value="">Tất cả Chi Nhánh</option>`;
        data.forEach(branch => {
            branchSelect.innerHTML += `
                <option value="${branch.id}">
                    ${branch.branchName || branch.name}
                </option>`;
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải chi nhánh:", err);
    }
}

// ===== Load danh sách combo =====
async function loadCombos() {
    try {
        const data = await comboApi.getAll();
        if (!data) return;
        allCombos = data;
        renderTable(data);
    } catch (err) {
        console.error("❌ Lỗi khi tải combo:", err);
    }
}

// ===== Kết hợp tất cả các bộ lọc =====
function handleFilters() {
    const branchId = document.getElementById("branchFilter").value;
    currentBranch = branchId;

    let filtered = [...allCombos];

    // 🔹 Lọc theo chi nhánh
    if (branchId) {
        filtered = filtered.filter(c => String(c.branchId) === String(branchId));
    }

    // 🔹 Lọc theo trạng thái
    if (currentAvailable === "1") {
        filtered = filtered.filter(c => c.available === true);
    } else if (currentAvailable === "0") {
        filtered = filtered.filter(c => c.available === false);
    }

    // 🔹 Lọc theo từ khóa (tên + mô tả + items + giá)
    if (currentKeyword) {
        const keyword = currentKeyword.replace(/[^\w\s]/g, ""); // loại bỏ ký tự đặc biệt
        filtered = filtered.filter(c => {
            const text = `${c.nameCombo} ${c.descriptionCombo || ""} ${c.items || ""}`.toLowerCase();
            const priceText = c.price ? c.price.toString().toLowerCase() : "";
            const formattedPrice = c.price ? c.price.toLocaleString("vi-VN") : "";

            // 🔍 So sánh cả chuỗi số và chuỗi hiển thị có dấu chấm
            return (
                text.includes(keyword) ||
                priceText.includes(keyword) ||
                formattedPrice.includes(keyword)
            );
        });
    }

    renderTable(filtered);
}

// ===== Xóa mềm combo =====
window.deleteCombo = async function (id) {
    if (!confirm("Bạn có chắc muốn ẩn combo này không?")) return;
    try {
        await comboApi.delete(id);
        alert("✅ Combo đã được ẩn (xóa mềm)!");
        await loadCombos();
        handleFilters();
    } catch (err) {
        console.error("❌ Lỗi khi xóa combo:", err);
        alert("Lỗi khi xóa combo!");
    }
};

// ===== Khôi phục combo =====
window.restoreCombo = async function (id) {
    if (!confirm("Khôi phục combo này?")) return;
    try {
        await comboApi.restore(id);
        alert("✅ Combo đã được khôi phục!");
        await loadCombos();
        handleFilters();
    } catch (err) {
        console.error("❌ Lỗi khi khôi phục combo:", err);
        alert("Lỗi khi khôi phục combo!");
    }
};

// ===== Render bảng =====
function renderTable(data) {
    if (dataTable) dataTable.destroy();

    const tbody = document.querySelector("#comboTable tbody");
    tbody.innerHTML = "";

    data.forEach(combo => {
        const imageUrl = combo.imageURL
            ? (combo.imageURL.startsWith("http")
                ? combo.imageURL
                : `http://localhost:8080${combo.imageURL}`)
            : null;

        tbody.innerHTML += `
            <tr>
                <td>
                    ${
            imageUrl
                ? `<img src="${imageUrl}" width="60" class="rounded">`
                : `<span class="text-muted">No Image</span>`
        }
                </td>
                <td>${combo.nameCombo}</td>
                <td>${combo.price.toLocaleString("vi-VN")} đ</td>
                <td title="${combo.descriptionCombo || ""}">${combo.descriptionCombo || ""}</td>
                <td title="${combo.items || ""}">${combo.items || ""}</td>
                <td>
                    <span class="status-dot ${combo.available ? "status-active" : "status-inactive"}"></span>
                </td>
                <td>${combo.branchName || ""}</td>
                <td>
                    <a href="updateCombo.html?id=${combo.id}" class="btn btn-warning btn-sm">Sửa</a>
                    ${
            combo.available
                ? `<button class="btn btn-danger btn-sm" onclick="deleteCombo(${combo.id})">Ẩn</button>`
                : `<button class="btn btn-success btn-sm" onclick="restoreCombo(${combo.id})">Khôi phục</button>`
        }
                </td>
            </tr>`;
    });

    dataTable = new DataTable("#comboTable", {
        paging: true,
        searching: false,
        info: false,
        pageLength: 10,
        lengthChange: false,
    });
}

// ===== Start =====
document.addEventListener("DOMContentLoaded", init);
