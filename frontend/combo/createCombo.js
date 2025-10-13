import { API_BASE_URL } from "../js/api.js";
import { requireAuth } from "../js/api/config.js";
import { comboApi } from "../js/api/comboApi.js";
import { branchApi } from "../js/api/branchApi.js";

const form = document.getElementById("comboForm");
const branchSelect = document.getElementById("branchId");
const imageInput = document.getElementById("imageFile");
const previewImg = document.getElementById("previewImg");
const result = document.getElementById("result");
const btnReset = document.getElementById("btnReset");

// ===== Khi trang load =====
document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;
    await loadBranches();
});

// ===== Load danh sách chi nhánh =====
async function loadBranches() {
    try {
        const branches = await branchApi.getNames();
        branchSelect.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
        branches.forEach(b => {
            branchSelect.innerHTML += `
                <option value="${b.id || b.branchId}">
                    ${b.name || b.branchName || "Không tên"}
                </option>`;
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải chi nhánh:", err);
        branchSelect.innerHTML = `<option value="">(Không tải được chi nhánh)</option>`;
    }
}

// ===== Preview ảnh khi chọn =====
imageInput.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (!file) {
        previewImg.style.display = "none";
        return;
    }
    previewImg.src = URL.createObjectURL(file);
    previewImg.style.display = "block";
});

// ===== Submit tạo combo mới =====
form.addEventListener("submit", async (e) => {
    e.preventDefault();
    result.textContent = "";

    // ✅ Lấy và kiểm tra branchId
    const branchIdValue = parseInt(branchSelect.value, 10);
    if (!branchIdValue || isNaN(branchIdValue)) {
        alert("⚠️ Vui lòng chọn chi nhánh hợp lệ!");
        return;
    }

    // ✅ Chuẩn bị dữ liệu combo
    const rawPrice = document.getElementById("price").value.replace(/\./g, "").replace(/,/g, "");
    const comboData = {
        branchId: branchIdValue,
        nameCombo: document.getElementById("nameCombo").value.trim(),
        price: parseFloat(rawPrice),
        descriptionCombo: document.getElementById("descriptionCombo").value.trim(),
        items: document.getElementById("items").value.trim(),
        available: document.getElementById("available").checked
    };

    const file = imageInput.files?.[0];

    console.log("📦 FormData gửi đi:", comboData);

    try {
        const response = await comboApi.create(comboData, file);
        if (!response) throw new Error("Không nhận được phản hồi từ server.");

        result.textContent = `✅ Tạo combo thành công: ${response.nameCombo}`;
        setTimeout(() => {
            window.location.href = "listCombo.html";
        }, 1000);
    } catch (err) {
        console.error("❌ API create failed:", err);
        result.textContent = "❌ Lỗi khi tạo combo!";
    }
});

// ===== Reset form =====
btnReset.addEventListener("click", () => {
    form.reset();
    previewImg.style.display = "none";
    result.textContent = "";
});
