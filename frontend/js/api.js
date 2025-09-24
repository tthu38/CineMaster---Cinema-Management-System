// --- API Base URL ---
// Khi deploy thì đổi thành domain BE thật (ví dụ: https://api.cinemaster.com/demo)
const BASE_URL = "http://localhost:8080/demo";

// --- Helper POST ---
export async function apiPost(path, body) {
    const res = await fetch(BASE_URL + path, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
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
        headers: { "Content-Type": "application/json" },
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

// --- Helper PUT ---
export async function apiPut(path, body) {
    const res = await fetch(BASE_URL + path, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

// --- Helper DELETE ---
export async function apiDelete(path) {
    const res = await fetch(BASE_URL + path, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}
