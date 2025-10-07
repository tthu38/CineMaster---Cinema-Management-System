const API_BASE_URL = "http://localhost:8080/api/v1/auth";

const form = document.getElementById("account-form");
const alertBox = document.getElementById("alert-box");

function showAlert(message, type = "success") {
    alertBox.innerHTML = `
    <div class="alert alert-${type}" role="alert">${message}</div>
  `;
}

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const email = document.getElementById("email").value.trim();
    const phone = document.getElementById("phone").value.trim();

    if (!email || !phone) {
        showAlert("⚠️ Vui lòng nhập đầy đủ email và số điện thoại!", "danger");
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/invite`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, phone })
        });

        const data = await res.json();
        if (res.ok) {
            showAlert("✅ " + data.message, "success");

            // 👇 Redirect về home sau 2 giây
            setTimeout(() => {
                window.location.href = "../home/home.html";
            }, 2000);

        } else {
            showAlert("❌ " + (data.message || "Không gửi được email"), "danger");
        }
    } catch (err) {
        console.error("Error:", err);
        showAlert("❌ Có lỗi xảy ra, vui lòng thử lại sau.", "danger");
    }
});
