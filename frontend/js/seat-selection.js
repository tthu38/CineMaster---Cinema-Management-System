// seat-selection.js

const seatingChart = document.querySelector('.seating-chart');
const selectedSeatsList = document.getElementById('selected-seats-list');
const totalPriceDisplay = document.getElementById('total-price-display');

// Dữ liệu Giá và Giá vé cơ bản
const SEAT_PRICES = { 'Normal': 1.00, 'VIP': 1.50, 'Couple': 2.00 };
const BASE_TICKET_PRICE = 150000;
let selectedSeats = [];

// Dữ liệu mô phỏng cho khoảng 50 ghế (A-E, 1-10)
const auditoriumSeatsData = [
    // HÀNG A (VIP)
    { SeatID: 1, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A1', SeatRow: 'A', ColumnNumber: 1, Status: 'Hold' }, // Tạm giữ
    { SeatID: 2, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A2', SeatRow: 'A', ColumnNumber: 2, Status: 'Available' },
    { SeatID: 3, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A3', SeatRow: 'A', ColumnNumber: 3, Status: 'Available' },
    { SeatID: 4, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A4', SeatRow: 'A', ColumnNumber: 4, Status: 'Reserved' },
    { SeatID: 5, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A5', SeatRow: 'A', ColumnNumber: 5, Status: 'Available' },
    { SeatID: 6, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A6', SeatRow: 'A', ColumnNumber: 6, Status: 'Available' },
    { SeatID: 7, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A7', SeatRow: 'A', ColumnNumber: 7, Status: 'Available' },
    { SeatID: 8, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A8', SeatRow: 'A', ColumnNumber: 8, Status: 'Available' },
    { SeatID: 9, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A9', SeatRow: 'A', ColumnNumber: 9, Status: 'Available' },
    { SeatID: 10, TypeID: 2, TypeName: 'VIP', SeatNumber: 'A10', SeatRow: 'A', ColumnNumber: 10, Status: 'Available' },

    // HÀNG B (NORMAL & COUPLE)
    { SeatID: 11, TypeID: 1, TypeName: 'Normal', SeatNumber: 'B1', SeatRow: 'B', ColumnNumber: 1, Status: 'Available' },
    { SeatID: 12, TypeID: 1, TypeName: 'Normal', SeatNumber: 'B2', SeatRow: 'B', ColumnNumber: 2, Status: 'Available' },
    { SeatID: 13, TypeID: 3, TypeName: 'Couple', SeatNumber: 'B3-4', SeatRow: 'B', ColumnNumber: 3, Status: 'Available' }, // Ghế đôi (chiếm 2 cột)
    { SeatID: 14, TypeID: 3, TypeName: 'Couple', SeatNumber: 'B5-6', SeatRow: 'B', ColumnNumber: 5, Status: 'Reserved' }, // Ghế đôi đã đặt
    { SeatID: 15, TypeID: 1, TypeName: 'Normal', SeatNumber: 'B7', SeatRow: 'B', ColumnNumber: 7, Status: 'Available' },
    { SeatID: 16, TypeID: 1, TypeName: 'Normal', SeatNumber: 'B8', SeatRow: 'B', ColumnNumber: 8, Status: 'Available' },
    { SeatID: 17, TypeID: 3, TypeName: 'Couple', SeatNumber: 'B9-10', SeatRow: 'B', ColumnNumber: 9, Status: 'Hold' }, // Ghế đôi Hold

    // HÀNG C (NORMAL)
    { SeatID: 18, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C1', SeatRow: 'C', ColumnNumber: 1, Status: 'Available' },
    { SeatID: 19, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C2', SeatRow: 'C', ColumnNumber: 2, Status: 'Available' },
    { SeatID: 20, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C3', SeatRow: 'C', ColumnNumber: 3, Status: 'Available' },
    { SeatID: 21, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C4', SeatRow: 'C', ColumnNumber: 4, Status: 'Available' },
    { SeatID: 22, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C5', SeatRow: 'C', ColumnNumber: 5, Status: 'Available' },
    { SeatID: 23, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C6', SeatRow: 'C', ColumnNumber: 6, Status: 'Available' },
    { SeatID: 24, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C7', SeatRow: 'C', ColumnNumber: 7, Status: 'Available' },
    { SeatID: 25, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C8', SeatRow: 'C', ColumnNumber: 8, Status: 'Available' },
    { SeatID: 26, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C9', SeatRow: 'C', ColumnNumber: 9, Status: 'Available' },
    { SeatID: 27, TypeID: 1, TypeName: 'Normal', SeatNumber: 'C10', SeatRow: 'C', ColumnNumber: 10, Status: 'Broken' },

    // HÀNG D (NORMAL)
    { SeatID: 28, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D1', SeatRow: 'D', ColumnNumber: 1, Status: 'Available' },
    { SeatID: 29, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D2', SeatRow: 'D', ColumnNumber: 2, Status: 'Available' },
    { SeatID: 30, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D3', SeatRow: 'D', ColumnNumber: 3, Status: 'Available' },
    { SeatID: 31, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D4', SeatRow: 'D', ColumnNumber: 4, Status: 'Available' },
    { SeatID: 32, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D5', SeatRow: 'D', ColumnNumber: 5, Status: 'Available' },
    { SeatID: 33, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D6', SeatRow: 'D', ColumnNumber: 6, Status: 'Available' },
    { SeatID: 34, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D7', SeatRow: 'D', ColumnNumber: 7, Status: 'Reserved' },
    { SeatID: 35, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D8', SeatRow: 'D', ColumnNumber: 8, Status: 'Available' },
    { SeatID: 36, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D9', SeatRow: 'D', ColumnNumber: 9, Status: 'Available' },
    { SeatID: 37, TypeID: 1, TypeName: 'Normal', SeatNumber: 'D10', SeatRow: 'D', ColumnNumber: 10, Status: 'Available' },

    // HÀNG E (NORMAL)
    { SeatID: 38, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E1', SeatRow: 'E', ColumnNumber: 1, Status: 'Available' },
    { SeatID: 39, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E2', SeatRow: 'E', ColumnNumber: 2, Status: 'Available' },
    { SeatID: 40, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E3', SeatRow: 'E', ColumnNumber: 3, Status: 'Available' },
    { SeatID: 41, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E4', SeatRow: 'E', ColumnNumber: 4, Status: 'Available' },
    { SeatID: 42, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E5', SeatRow: 'E', ColumnNumber: 5, Status: 'Available' },
    { SeatID: 43, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E6', SeatRow: 'E', ColumnNumber: 6, Status: 'Available' },
    { SeatID: 44, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E7', SeatRow: 'E', ColumnNumber: 7, Status: 'Hold' },
    { SeatID: 45, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E8', SeatRow: 'E', ColumnNumber: 8, Status: 'Available' },
    { SeatID: 46, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E9', SeatRow: 'E', ColumnNumber: 9, Status: 'Available' },
    { SeatID: 47, TypeID: 1, TypeName: 'Normal', SeatNumber: 'E10', SeatRow: 'E', ColumnNumber: 10, Status: 'Available' },
];

/**
 * 1. Hiển thị sơ đồ ghế từ dữ liệu
 */
function renderSeatingChart() {
    // Nhóm ghế theo hàng
    const rows = auditoriumSeatsData.reduce((acc, seat) => {
        if (!acc[seat.SeatRow]) { acc[seat.SeatRow] = []; }
        acc[seat.SeatRow].push(seat);
        return acc;
    }, {});

    seatingChart.innerHTML = '';

    for (const [rowName, seats] of Object.entries(rows)) {
        const rowDiv = document.createElement('div');
        rowDiv.classList.add('seat-row');

        // Thêm nhãn hàng (A, B, C...)
        const label = document.createElement('div');
        label.classList.add('seat-label');
        label.textContent = rowName;
        rowDiv.appendChild(label);

        // Tạo từng ghế
        seats.sort((a, b) => a.ColumnNumber - b.ColumnNumber).forEach(seatData => {
            const seatDiv = document.createElement('div');
            // Class chung và class loại ghế (vip, couple, normal)
            seatDiv.classList.add('seat', seatData.TypeName.toLowerCase());

            seatDiv.dataset.seatId = seatData.SeatID;
            seatDiv.dataset.priceMultiplier = SEAT_PRICES[seatData.TypeName];
            seatDiv.dataset.typeName = seatData.TypeName;
            seatDiv.textContent = seatData.SeatNumber.replace(/-.*/, ''); // Hiển thị số ghế (A1, B3, C9)

            // LOGIC TRẠNG THÁI
            if (seatData.Status === 'Reserved' || seatData.Status === 'Broken') {
                seatDiv.classList.add('reserved');
            } else if (seatData.Status === 'Hold') {
                seatDiv.classList.add('hold');
            } else {
                // Status = 'Available'
                seatDiv.addEventListener('click', toggleSeatSelection);
            }

            rowDiv.appendChild(seatDiv);
        });

        seatingChart.appendChild(rowDiv);
    }
}

/**
 * 2. Xử lý logic chọn/bỏ chọn ghế và tính toán tiền
 */
function toggleSeatSelection(event) {
    const seatDiv = event.target;

    // Ngăn chặn chọn nếu không phải Available
    if (seatDiv.classList.contains('reserved') || seatDiv.classList.contains('broken') || seatDiv.classList.contains('hold')) {
        return;
    }

    // Nếu chọn ghế đôi, chỉ tính 1 vé
    const isCoupleSeat = seatDiv.dataset.typeName === 'Couple';
    const maxSeats = 8; // Giới hạn số lượng vé tối đa là 8

    if (seatDiv.classList.contains('selected')) {
        // BỎ CHỌN
        seatDiv.classList.remove('selected');
        const seatId = parseInt(seatDiv.dataset.seatId);
        selectedSeats = selectedSeats.filter(seat => seat.id !== seatId);

        console.log(`[Frontend Action]: Đã bỏ chọn ghế ${seatDiv.textContent}.`);

    } else {
        // CHỌN
        // Kiểm tra giới hạn ghế
        if (selectedSeats.length >= maxSeats) {
            alert(`Bạn chỉ có thể chọn tối đa ${maxSeats} ghế.`);
            return;
        }

        seatDiv.classList.add('selected');

        const seatId = parseInt(seatDiv.dataset.seatId);
        const priceMultiplier = parseFloat(seatDiv.dataset.priceMultiplier);
        const seatName = seatDiv.textContent;
        const typeName = seatDiv.dataset.typeName;

        selectedSeats.push({
            id: seatId,
            name: seatName + (isCoupleSeat ? ' (Đôi)' : ''),
            multiplier: priceMultiplier,
            typeName: typeName,
            price: BASE_TICKET_PRICE * priceMultiplier
        });

        console.log(`[Frontend Action]: Đã chọn ghế ${seatName}. (Cần gọi API Backend để Hold)`);
    }

    updateSummary();
}

/**
 * 3. Cập nhật thông tin tổng hợp (Ghế đã chọn và Tổng tiền)
 */
function updateSummary() {
    let totalPrice = 0;
    selectedSeatsList.innerHTML = '';

    if (selectedSeats.length === 0) {
        selectedSeatsList.innerHTML = '<p class="text-muted" id="no-seat-selected">Chưa có ghế nào được chọn.</p>';
        totalPriceDisplay.textContent = formatCurrency(0);
        return;
    }

    // Sắp xếp ghế theo ID để hiển thị thứ tự hợp lý
    selectedSeats.sort((a, b) => a.id - b.id).forEach(seat => {
        totalPrice += seat.price;

        // Tạo HTML cho từng mục ghế
        const seatDetail = document.createElement('div');
        seatDetail.classList.add('seat-item', 'd-flex', 'justify-content-between');
        seatDetail.innerHTML = `
            <span class="seat-name">${seat.name}</span>
            <span class="text-muted small">${seat.typeName}</span>
            <span class="seat-price">${formatCurrency(seat.price)}</span>
        `;
        selectedSeatsList.appendChild(seatDetail);
    });

    totalPriceDisplay.textContent = formatCurrency(totalPrice);
}

/**
 * Hàm định dạng tiền tệ (VNĐ)
 */
function formatCurrency(amount) {
    return amount.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
}

// KHỞI TẠO
document.addEventListener('DOMContentLoaded', () => {
    renderSeatingChart();
    updateSummary();
});