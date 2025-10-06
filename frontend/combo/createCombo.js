import { comboApi, branchApi, requireAuth } from "../js/api.js";

const form = document.getElementById("comboForm");
const branchSelect = document.getElementById("branchId");
const imageInput = document.getElementById("imageFile");
const previewImg = document.getElementById("previewImg");
const result = document.getElementById("result");
const btnReset = document.getElementById("btnReset");

// ========== INIT ==========
document.addEventListener("DOMContentLoaded", async () => {
    if (!requireAuth()) return;
    await loadBranches();
});

// ========== LOAD BRANCHES ==========
async function loadBranches() {
    try {
        const data = await branchApi.getAll();
        if (!data) return;
        branchSelect.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
        data.forEach(b => {
            branchSelect.innerHTML += `<option value="${b.id}">${b.name}</option>`;
        });
    } catch (err) {
        console.error("Error loading branches:", err);
        branchSelect.innerHTML = `<option value="">(Không tải được chi nhánh)</option>`;
    }
}

// ========== PREVIEW IMAGE ==========
imageInput.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (!file) {
        previewImg.style.display = "none";
        return;
    }
    previewImg.src = URL.createObjectURL(file);
    previewImg.style.display = "block";
});

// ========== SUBMIT ==========
form.addEventListener("submit", async (e) => {
    e.preventDefault();
    result.textContent = "";

    const comboData = {
        branchId: parseInt(branchSelect.value, 10),
        nameCombo: document.getElementById("nameCombo").value,
        price: parseFloat(document.getElementById("price").value),
        descriptionCombo: document.getElementById("descriptionCombo").value,
        items: document.getElementById("items").value,
        available: document.getElementById("available").checked
    };

    const file = imageInput.files?.[0];

    try {
        const data = await comboApi.create(comboData, file);

        // Thông báo ngắn gọn trước khi chuyển trang
        result.textContent = `✅ Tạo combo thành công: ${data.nameCombo}`;

        // ✅ Delay 1 chút cho người dùng thấy thông báo (optional)
        setTimeout(() => {
            window.location.href = "listCombo.html"; // ← điều hướng về trang danh sách combo
        }, 800);
    } catch (err) {
        console.error(err);
        result.textContent = "❌ Lỗi khi tạo combo!";
    }

});

// ========== RESET ==========
btnReset.addEventListener("click", () => {
    form.reset();
    previewImg.style.display = "none";
    result.textContent = "";
});
