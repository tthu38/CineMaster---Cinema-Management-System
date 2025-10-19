import { seatApi } from "./api/seatApi.js";
import { ticketApi } from "./api/ticketApi.js";
import { showtimeApi } from "./api/showtimeApi.js";
import { requireAuth } from "./api/config.js";

requireAuth();

const params = new URLSearchParams(window.location.search);
const showtimeId = params.get("showtimeId");

const seatingChart = document.querySelector('.seating-chart');
const selectedSeatsList = document.getElementById('selected-seats-list');
const totalPriceDisplay = document.getElementById('total-price-display');
const btnContinue = document.querySelector('.btn-continue');

let showtime = null;
let selectedSeats = [];
const SEAT_PRICES = { 'NORMAL': 1.0, 'VIP': 1.5, 'COUPLE': 2.0 };

document.addEventListener("DOMContentLoaded", async () => {
    if (!showtimeId) {
        alert("❌ Thiếu ID suất chiếu!");
        return;
    }
    await loadShowtimeAndSeats();
});

// === 1️⃣ Lấy suất chiếu và sơ đồ ghế tương ứng ===
async function loadShowtimeAndSeats() {
    try {
        const showtime = await showtimeApi.getPublicById(showtimeId);
        document.querySelector(".movie-title-large").textContent = showtime.movie.title;
        document.querySelector(".showtime-details").innerHTML =
            `<i class="far fa-clock"></i> ${showtime.startTime.slice(11,16)} - ${showtime.endTime.slice(11,16)} |
       <i class="fas fa-calendar-alt"></i> ${new Date(showtime.startTime).toLocaleDateString('vi-VN')}`;
        document.querySelector(".branch-info").textContent =
            `${showtime.branch.branchName} | ${showtime.auditorium.name}`;

        const auditoriumId = showtime.auditorium.auditoriumID;
        const seats = await seatApi.getAllByAuditorium(auditoriumId);
        renderSeats(seats, showtime.basePrice);

        // 🟢 ====> THÊM 4 DÒNG NÀY Ở ĐÂY <====
        document.getElementById("loading").style.display = "none";
        document.querySelector(".screen-container").style.display = "block";
        document.querySelector(".seating-chart").style.display = "flex";
        document.querySelector(".legend").style.display = "flex";
        // 🟢 ================================
    } catch (err) {
        console.error("❌ Lỗi load suất chiếu:", err);
    }
}


// === 2️⃣ Vẽ sơ đồ ghế ===
function renderSeats(seats, basePrice) {
    const rows = seats.reduce((acc, s) => {
        if (!acc[s.seatRow]) acc[s.seatRow] = [];
        acc[s.seatRow].push(s);
        return acc;
    }, {});
    seatingChart.innerHTML = '';

    Object.entries(rows).forEach(([row, list]) => {
        const rowDiv = document.createElement('div');
        rowDiv.classList.add('seat-row');

        const label = document.createElement('div');
        label.classList.add('seat-label');
        label.textContent = row;
        rowDiv.appendChild(label);

        list.sort((a,b)=>a.columnNumber-b.columnNumber).forEach(seat=>{
            const div = document.createElement('div');
            div.classList.add('seat');
            div.textContent = seat.seatNumber;
            div.dataset.id = seat.seatID;
            div.dataset.type = seat.seatType.typeName.toUpperCase();
            div.dataset.multiplier = seat.seatType.priceMultiplier;

            // set màu
            if (seat.seatType.typeName === "VIP") div.classList.add("vip");
            if (seat.seatType.typeName === "Couple") div.classList.add("couple");

            switch (seat.status.toUpperCase()) {
                case "BOOKED": case "RESERVED": case "BROKEN":
                    div.classList.add("reserved");
                    break;
                default:
                    div.addEventListener("click", () => toggleSeat(div, basePrice));
            }

            rowDiv.appendChild(div);
        });
        seatingChart.appendChild(rowDiv);
    });
}

// === 3️⃣ Toggle chọn ghế ===
function toggleSeat(div, basePrice) {
    const id = parseInt(div.dataset.id);
    const type = div.dataset.type;
    const multiplier = parseFloat(div.dataset.multiplier);
    const found = selectedSeats.find(s=>s.id===id);

    if(found){
        selectedSeats = selectedSeats.filter(s=>s.id!==id);
        div.classList.remove("selected");
    } else {
        selectedSeats.push({
            id, name: div.textContent, type, price: basePrice * multiplier
        });
        div.classList.add("selected");
    }
    updateSummary();
}

// === 4️⃣ Cập nhật panel trái ===
function updateSummary(){
    selectedSeatsList.innerHTML='';
    let total = 0;
    selectedSeats.forEach(s=>{
        const div = document.createElement('div');
        div.classList.add("seat-item","d-flex","justify-content-between");
        div.innerHTML = `
      <span class="seat-name">${s.name}</span>
      <span class="text-muted small">${s.type}</span>
      <span class="seat-price">${s.price.toLocaleString()} đ</span>`;
        total += s.price;
        selectedSeatsList.appendChild(div);
    });
    totalPriceDisplay.textContent = total.toLocaleString()+" VNĐ";
}

// === 5️⃣ Đặt vé ===
btnContinue.addEventListener("click", async()=>{
    if(selectedSeats.length===0) return alert("Bạn chưa chọn ghế!");
    const accountId = localStorage.getItem("accountId");
    const body = {
        accountId: parseInt(accountId),
        showtimeId: parseInt(showtimeId),
        seatIds: selectedSeats.map(s=>s.id),
        paymentMethod:"Cash"
    };
    try{
        const ticket = await ticketApi.bookTicket(body);
        alert("🎟️ Đặt vé thành công!");
        window.location.href = `/frontend/user/ticket-detail.html?id=${ticket.ticketID}`;
    }catch(err){
        alert("❌ Lỗi đặt vé!");
        console.error(err);
    }
});
