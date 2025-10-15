package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.TicketBookingRequest;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketSeatRepository ticketSeatRepo;
    private final TicketDiscountRepository ticketDiscountRepo;
    private final SeatRepository seatRepo;
    private final ComboRepository comboRepo;
    private final DiscountRepository discountRepo;
    private final AccountRepository accountRepo;
    private final ShowtimeRepository showtimeRepo;

    // ===== ĐẶT VÉ =====
    @Transactional
    public Ticket bookTicket(TicketBookingRequest req) {
        Account acc = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        Showtime showtime = showtimeRepo.findById(req.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Suất chiếu không tồn tại"));

        BigDecimal total = BigDecimal.ZERO;

        Ticket ticket = Ticket.builder()
                .account(acc)
                .showtime(showtime)
                .bookingTime(LocalDateTime.now())
                .ticketStatus("Pending")
                .paymentMethod(req.getPaymentMethod())
                .totalPrice(BigDecimal.ZERO)
                .build();
        ticketRepo.save(ticket);

        for (Integer seatId : req.getSeatIds()) {
            Seat seat = seatRepo.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Ghế không tồn tại: " + seatId));

            if (!"Available".equalsIgnoreCase(seat.getStatus()))
                throw new RuntimeException("Ghế " + seat.getSeatNumber() + " đã bị giữ!");

            seat.setStatus("Reserved");
            seat.setLockedUntil(LocalDateTime.now().plusMinutes(10));
            seatRepo.save(seat);

            TicketSeat ts = TicketSeat.builder()
                    .id(new TicketSeatId(ticket.getTicketID(), seat.getSeatID()))
                    .ticket(ticket)
                    .seat(seat)
                    .build();
            ticketSeatRepo.save(ts);

            BigDecimal price = showtime.getBasePrice().multiply(seat.getSeatType().getPriceMultiplier());
            total = total.add(price);
        }

        if (req.getComboId() != null) {
            Combo combo = comboRepo.findById(req.getComboId())
                    .orElseThrow(() -> new RuntimeException("Combo không tồn tại"));
            ticket.setCombo(combo);
            total = total.add(combo.getPrice());
        }

        if (req.getDiscountCode() != null) {
            Discount discount = discountRepo.findByDiscountCode(req.getDiscountCode())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
            total = total.subtract(discount.getDiscountAmount());
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

            TicketDiscount td = TicketDiscount.builder()
                    .id(new TicketDiscountId(ticket.getTicketID(), discount.getDiscountID()))
                    .ticketID(ticket)
                    .discountID(discount)
                    .amount(discount.getDiscountAmount())
                    .build();
            ticketDiscountRepo.save(td);
        }

        ticket.setTotalPrice(total);
        ticket.setTicketStatus("Booked");
        ticketRepo.save(ticket);
        return ticket;
    }

    // ===== LẤY CHI TIẾT VÉ =====
    public Ticket getTicketDetail(Integer ticketId) {
        return ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));
    }

    // ===== HỦY VÉ =====
    @Transactional
    public void cancelTicket(Integer ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));
        if ("Cancelled".equals(t.getTicketStatus())) return;

        if (t.getShowtime().getStartTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Không thể hủy vé sau giờ chiếu");

        t.setTicketStatus("Cancelled");
        ticketRepo.save(t);

        for (TicketSeat ts : t.getTicketSeats()) {
            Seat s = ts.getSeat();
            s.setStatus("Available");
            s.setLockedUntil(null);
            seatRepo.save(s);
        }

        ticketDiscountRepo.deleteAll(t.getTicketDiscounts());
    }

    // ===== LẤY DANH SÁCH VÉ NGƯỜI DÙNG =====
    public List<Ticket> getTicketsByAccount(Integer accountId) {
        return ticketRepo.findAllByAccount_AccountID(accountId);
    }
}
