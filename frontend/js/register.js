// API Backend (Spring Boot)
const API_BASE = "http://localhost:8080/api/v1/auth";

document.getElementById("registerForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const fullName = document.getElementById("fullName").value.trim();
    const email = document.getElementById("email").value.trim();
    const phoneNumber = document.getElementById("phoneNumber").value.trim();
    const password = document.getElementById("password").value;
    const resultBox = document.getElementById("result");

    // Reset thông báo cũ
    resultBox.textContent = "";
    resultBox.style.color = "#333";

    const data = { fullName, email, phoneNumber, password };

    try {
        const res = await fetch(`${API_BASE}/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });

        const text = await res.text();

        if (res.ok) {
            resultBox.innerText = "Đăng ký thành công! Vui lòng xác thực email.";
            resultBox.style.color = "#4caf50";

            // Sau 1s chuyển qua verify.html cùng thư mục
            setTimeout(() => {
                window.location.href = "./verify.html?email=" + encodeURIComponent(email);
            }, 1000);
        } else {
            resultBox.innerText = "Đăng ký thất bại: " + (text || res.status);
            resultBox.style.color = "#e53935";
        }
    } catch (err) {
        resultBox.innerText = "Lỗi kết nối: " + err.message;
        resultBox.style.color = "#e53935";
    }
});
