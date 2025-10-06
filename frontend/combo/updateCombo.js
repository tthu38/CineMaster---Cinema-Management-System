import { comboApi, branchApi, requireAuth, API_BASE_URL } from "../js/api.js";

const form = document.getElementById("comboForm");
const branchSelect = document.getElementById("branchId");
const imageInput = document.getElementById("imageFile");
const previewImg = document.getElementById("previewImg");
const result = document.getElementById("result");

// 🧩 Lấy id combo từ URL (?id=...)
const params = new URLSearchParams(window.location.search);
const comboId = params.get("id");

// ===== Khởi động =====
document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;
    await loadBranches();
    if (comboId) await loadComboDetail(comboId);
});

// ===== Load danh sách chi nhánh =====
async function loadBranches() {
    try {
        const branches = await branchApi.getAll();
        branchSelect.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
        branches.forEach(b => {
            branchSelect.innerHTML += `<option value="${b.id}">${b.branchName || b.name}</option>`;
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải chi nhánh:", err);
    }
}

// ===== Load thông tin combo =====
async function loadComboDetail(id) {
    try {
        const combo = await comboApi.getById(id);
        console.log("🎬 Combo detail:", combo);

        document.getElementById("nameCombo").value = combo.nameCombo || "";
        document.getElementById("price").value = combo.price
            ? combo.price.toLocaleString("vi-VN")
            : "";
        document.getElementById("descriptionCombo").value = combo.descriptionCombo || "";
        document.getElementById("items").value = combo.items || "";
        document.getElementById("available").checked = combo.available ?? false;
        branchSelect.value = combo.branchId || "";

        if (combo.imageURL) {
            const baseURL = API_BASE_URL.replace("/api/v1", "");
            previewImg.src = combo.imageURL.startsWith("http")
                ? combo.imageURL
                : `${baseURL}${combo.imageURL}`;
            previewImg.style.display = "block";
        } else {
            previewImg.style.display = "none";
        }
    } catch (err) {
        console.error("❌ Error loading combo:", err);
    }
}

// ===== Xem trước ảnh mới =====
imageInput.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (!file) {
        previewImg.style.display = "none";
        return;
    }
    previewImg.src = URL.createObjectURL(file);
    previewImg.style.display = "block";
});

// ===== Submit form cập nhật combo =====
form.addEventListener("submit", async (e) => {
    e.preventDefault();
    result.textContent = "";

    const rawPrice = document.getElementById("price").value.replace(/\./g, "").replace(/,/g, "");
    const comboData = {
        branchId: parseInt(branchSelect.value, 10),
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
        console.error(err);
        result.textContent = "❌ Lỗi khi cập nhật combo!";
    }
});
