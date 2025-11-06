import { chatApi } from "../js/api/chatApi.js";

const chatBody = document.getElementById("chatBody");
const chatInput = document.getElementById("chatInput");
const sendBtn = document.getElementById("sendBtn");

sendBtn.addEventListener("click", sendMessage);
chatInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") sendMessage();
});

async function sendMessage() {
    const question = chatInput.value.trim();
    if (!question) return;

    // 🧍 Hiển thị tin nhắn người dùng
    addMessage(question, "user");
    chatInput.value = "";

    // 💬 Hiển thị hiệu ứng loading
    const loading = addLoading();

    try {
        // 🚀 Gọi API backend
        const answer = await chatApi.ask(question);
        console.log("BOT RESPONSE:", answer);

        // ✅ Xóa loading, thêm tin nhắn bot (markdown)
        removeLoading(loading);
        addMessage(answer, "bot");
    } catch (err) {
        removeLoading(loading);
        addMessage("⚠️ Lỗi: " + err.message, "bot");
    }
}

/** 🧩 Thêm tin nhắn (hỗ trợ Markdown cho bot) */
function addMessage(text, type) {
    const msg = document.createElement("div");
    msg.classList.add("bubble", type);

    if (type === "bot") {
        // ⚙️ Cấu hình Marked để hiển thị markdown đúng chuẩn
        marked.setOptions({
            breaks: true,        // Cho phép xuống dòng
            mangle: false,       // Giữ nguyên ký tự trong link
            headerIds: false     // Không tạo id tự động cho tiêu đề
        });

        // ✅ Parse markdown (in đậm, link, v.v.)
        msg.innerHTML = marked.parse(text);

        // 🎨 Biến tất cả link thành nút đẹp
        msg.querySelectorAll("a").forEach(a => {
            a.classList.add("btn", "btn-sm", "btn-primary", "mt-2");
            a.target = "_blank"; // mở tab mới
            a.style.textDecoration = "none";
            a.style.color = "#fff";
            a.style.fontWeight = "600";
        });
    } else {
        // 🧍 Tin nhắn người dùng: chỉ là text bình thường
        msg.textContent = text;
    }

    chatBody.appendChild(msg);
    chatBody.scrollTop = chatBody.scrollHeight;
    return msg;
}

/** 💫 Hiệu ứng loading bot */
function addLoading() {
    const msg = document.createElement("div");
    msg.classList.add("bubble", "bot");
    msg.innerHTML = `
    <div class="loading">
      <span>.</span><span>.</span><span>.</span>
    </div>
  `;
    chatBody.appendChild(msg);
    chatBody.scrollTop = chatBody.scrollHeight;
    return msg;
}

/** ❌ Xóa loading */
function removeLoading(el) {
    if (el && el.parentNode) el.parentNode.removeChild(el);
}
