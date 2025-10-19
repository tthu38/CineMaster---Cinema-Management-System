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
        // ✅ Dùng thư viện marked để parse Markdown
        msg.innerHTML = marked.parse(text);
    } else {
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
