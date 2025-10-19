import { API_BASE_URL, handleResponse, getValidToken } from "../js/api/config.js";

document.addEventListener("DOMContentLoaded", async () => {
    const token = getValidToken();
    const params = new URLSearchParams(window.location.search);
    const id = params.get("contactId");

    if (!token || !id) {
        Swal.fire("Lỗi","Không thể xác định liên hệ!","error");
        window.location.href="contactManagement.html";
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/contacts/${id}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const c = await handleResponse(res);
        render(c);
    } catch (err) {
        Swal.fire("❌ Lỗi", err.message || "Không tải được thông tin liên hệ.","error");
    }
});

function render(c) {
    document.getElementById("fullName").textContent = c.fullName;
    document.getElementById("email").textContent = c.email;
    document.getElementById("phone").textContent = c.phone || "—";
    document.getElementById("branchName").textContent = c.branchName || "—";
    document.getElementById("subject").textContent = c.subject;
    document.getElementById("message").textContent = c.message;
    document.getElementById("status").textContent = c.status;
    document.getElementById("status").classList.add(`status-${c.status}`);
    document.getElementById("handledBy").textContent = c.handledBy || "Chưa có";
    document.getElementById("handledAt").textContent = c.handledAt ? c.handledAt.replace("T"," ") : "—";
    document.getElementById("handleNote").textContent = c.handleNote || "—";
}
