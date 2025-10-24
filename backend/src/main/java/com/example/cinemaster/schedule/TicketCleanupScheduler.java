//package com.example.cinemaster.schedule;
//
//import com.example.cinemaster.entity.Seat;
//import com.example.cinemaster.entity.Ticket;
//import com.example.cinemaster.entity.TicketSeat;
//import com.example.cinemaster.repository.SeatRepository;
//import com.example.cinemaster.repository.TicketRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TicketCleanupScheduler {
//
//    private final TicketRepository ticketRepo;
//    private final SeatRepository seatRepo;
//
//    @Value("${ticket.pending-timeout-minutes:10}")
//    private int pendingTimeoutMinutes;
//
//    @Value("${ticket.cleanup-interval-seconds:60}")
//    private int cleanupIntervalSeconds;
//
//    /** 🔁 Hủy vé chưa thanh toán quá thời gian quy định (default: 10 phút) */
//    @Scheduled(fixedRateString = "${ticket.cleanup-interval-seconds:60}000")
//    public void cancelUnpaidTickets() {
//        LocalDateTime limit = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
//
//        List<Ticket> expiredTickets =
//                ticketRepo.findByTicketStatusAndBookingTimeBefore("Pending", limit);
//        if (expiredTickets.isEmpty()) return;
//
//        for (Ticket ticket : expiredTickets) {
//            if (ticket.getTicketSeats() != null) {
//                for (TicketSeat ts : ticket.getTicketSeats()) {
//                    Seat seat = ts.getSeat();
//                    if (seat != null) {
//                        seat.setStatus(Seat.SeatStatus.AVAILABLE);
//                        seat.setLockedUntil(null);
//                        seatRepo.save(seat);
//                        log.info("[Scheduler] 🔓 Giải phóng ghế '{}' thuộc vé #{}", seat.getSeatNumber(), ticket.getTicketID());
//                    }
//                }
//            }
//
//            ticket.setTicketStatus("CANCELLED");
//            ticketRepo.save(ticket);
//            log.info("[Scheduler] 🕐 Vé #{} bị hủy (Pending quá hạn {} phút)", ticket.getTicketID(), pendingTimeoutMinutes);
//        }
//    }
//
//    /** 🔁 Giải phóng ghế đã hết thời gian giữ (status = RESERVED) */
//    @Scheduled(fixedRateString = "${ticket.cleanup-interval-seconds:60}000")
//    public void releaseExpiredSeatLocks() {
//        LocalDateTime now = LocalDateTime.now();
//        List<Seat> expiredSeats =
//                seatRepo.findByStatusAndLockedUntilBefore(Seat.SeatStatus.RESERVED, now);
//        if (expiredSeats.isEmpty()) return;
//
//        for (Seat seat : expiredSeats) {
//            seat.setStatus(Seat.SeatStatus.AVAILABLE);
//            seat.setLockedUntil(null);
//            seatRepo.save(seat);
//            log.info("[Scheduler] 🔓 Ghế '{}' được giải phóng (hết thời gian giữ chỗ)", seat.getSeatNumber());
//        }
//    }
//}
