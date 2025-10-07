const API_BASE = "http://localhost:8080/api/v1/auth";

const passwordInput = document.getElementById("password");
const registerBtn = document.getElementById("registerBtn");

// password rules
const ruleLength = document.getElementById("rule-length");
const ruleUppercase = document.getElementById("rule-uppercase");
const ruleNumber = document.getElementById("rule-number");
const ruleSpecial = document.getElementById("rule-special");

function validatePassword(pw) {
    let valid = true;

    // >= 8 ký tự
    if (pw.length >= 8) ruleLength.classList.add("valid");
    else { ruleLength.classList.remove("valid"); valid = false; }

    // bắt đầu bằng chữ in hoa
    if (/^[A-Z]/.test(pw)) ruleUppercase.classList.add("valid");
    else { ruleUppercase.classList.remove("valid"); valid = false; }

    // chứa số
    if (/\d/.test(pw)) ruleNumber.classList.add("valid");
    else { ruleNumber.classList.remove("valid"); valid = false; }

    // chứa ký tự đặc biệt
    if (/[!@#$%^&*()_\-+={}[\]:;"'<>,.?/]/.test(pw)) ruleSpecial.classList.add("valid");
    else { ruleSpecial.classList.remove("valid"); valid = false; }

    registerBtn.disabled = !valid;
}

passwordInput.addEventListener("input", (e) => {
    validatePassword(e.target.value);
});

document.getElementById("registerForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const fullName = document.getElementById("fullName").value.trim();
    const email = document.getElementById("email").value.trim();
    const phoneNumber = document.getElementById("phoneNumber").value.trim();
    const password = passwordInput.value;
    const resultBox = document.getElementById("result");

    resultBox.textContent = "";
    resultBox.style.color = "#fff";

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
