import { API_BASE_URL } from "../js/api.js";
import { requireAuth } from "../js/api/config.js";
import { comboApi } from "../js/api/comboApi.js";
import { branchApi } from "../js/api/branchApi.js";

const form = document.getElementById("comboForm");
const branchSelect = document.getElementById("branchId");
const imageInput = document.getElementById("imageFile");
const previewImg = document.getElementById("previewImg");
const result = document.getElementById("result");
const params = new URLSearchParams(window.location.search);
const comboId = params.get("id");
const btnChangeImage = document.getElementById("btnChangeImage");

let comboCache = null;

// ===== Khi trang load =====
document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;
    await loadBranches();
    if (comboId) await loadComboDetail(comboId);

    // 🔹 Cho phép click icon camera để chọn ảnh
    if (btnChangeImage) {
        btnChangeImage.addEventListener("click", () => imageInput.click());
    }
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

        if (comboCache && comboCache.branchId) {
            branchSelect.value = comboCache.branchId;
        }
    } catch (err) {
        console.error("❌ Lỗi khi tải chi nhánh:", err);
    }
}

// ===== Load combo detail =====
async function loadComboDetail(id) {
    try {
        const combo = await comboApi.getById(id);
        comboCache = combo;
        console.log("🎬 Combo detail:", combo);

        document.getElementById("nameCombo").value = combo.nameCombo || "";
        document.getElementById("price").value = combo.price ?? "";
        document.getElementById("descriptionCombo").value = combo.descriptionCombo || "";
        document.getElementById("items").value = combo.items || "";
        document.getElementById("available").checked = combo.available ?? false;
        branchSelect.value = combo.branchId || "";

        // Ảnh preview
        if (combo.imageURL) {
            const baseURL = API_BASE_URL.replace("/api/v1", "");
            previewImg.src = combo.imageURL.startsWith("http") ? combo.imageURL : `${baseURL}${combo.imageURL}`;
            previewImg.style.display = "block";
        } else {
            previewImg.style.display = "none";
        }
    } catch (err) {
        console.error("❌ Error loading combo:", err);
    }
}

// ===== Preview ảnh mới =====
imageInput.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (!file) return (previewImg.style.display = "none");
    previewImg.src = URL.createObjectURL(file);
    previewImg.style.display = "block";
});

// ===== Submit update combo =====
form.addEventListener("submit", async (e) => {
    e.preventDefault();
    result.textContent = "";

    const branchIdValue = parseInt(branchSelect.value, 10);
    if (!branchIdValue || isNaN(branchIdValue)) {
        alert("❌ Vui lòng chọn chi nhánh hợp lệ!");
        return;
    }

    const rawPrice = document.getElementById("price").value.replace(/[^\d]/g, "");
    const comboData = {
        branchId: branchIdValue,
        nameCombo: document.getElementById("nameCombo").value.trim(),
        price: parseFloat(rawPrice),
        descriptionCombo: document.getElementById("descriptionCombo").value.trim(),
        items: document.getElementById("items").value.trim(),
        available: document.getElementById("available").checked
    };

    const file = imageInput.files?.[0];
    try {
        await comboApi.update(comboId, comboData, file);
        result.textContent = "✅ Cập nhật thành công!";
        setTimeout(() => (window.location.href = "listCombo.html"), 1000);
    } catch (err) {
        console.error("❌ Lỗi khi cập nhật combo:", err);
        result.textContent = "❌ Lỗi khi cập nhật combo!";
    }
});
