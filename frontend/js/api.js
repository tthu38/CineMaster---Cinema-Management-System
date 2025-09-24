// --- API Base URL ---
// Khi deploy thì đổi thành domain BE thật (ví dụ: https://api.cinemaster.com/demo)
const BASE_URL = "http://localhost:8080/demo";

// --- Helper: lấy headers chung ---
function getHeaders(isJson = true) {
    const headers = {};
    if (isJson) headers["Content-Type"] = "application/json";

    const token = localStorage.getItem("accessToken");
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }
    return headers;
}

// --- Helper POST ---
export async function apiPost(path, body) {
    const res = await fetch(BASE_URL + path, {
        method: "POST",
        headers: getHeaders(true),
        body: JSON.stringify(body),
    });

    if (!res.ok) {
        const contentType = res.headers.get("content-type");
        let errMsg;

        if (contentType && contentType.includes("application/json")) {
            const errorData = await res.json();
            errMsg = errorData.message || JSON.stringify(errorData);
        } else {
            errMsg = await res.text();
        }

        throw new Error(errMsg || `HTTP ${res.status}`);
    }

    return res.json();
}

// --- Helper GET ---
export async function apiGet(path) {
    const res = await fetch(BASE_URL + path, {
        method: "GET",
        headers: getHeaders(true),
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

// --- Helper PUT ---
export async function apiPut(path, body) {
    const res = await fetch(BASE_URL + path, {
        method: "PUT",
        headers: getHeaders(true),
        body: JSON.stringify(body),
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

// --- Helper DELETE ---
export async function apiDelete(path) {
    const res = await fetch(BASE_URL + path, {
        method: "DELETE",
        headers: getHeaders(true),
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}
