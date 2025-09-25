const LOGIN_PAGE = "../user/login.html";

window.handleLogout = async function handleLogout() {
    try {
        const token = localStorage.getItem("accessToken");
        if (token) {
            await fetch("http://localhost:8080/demo/api/auth/logout", {
                method: "POST",
                headers: { "Authorization": `Bearer ${token}` }
            });
        }
    } catch (e) {
        console.error("Logout error:", e);
    } finally {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userInfo");
        window.location.href = LOGIN_PAGE;
    }
};

