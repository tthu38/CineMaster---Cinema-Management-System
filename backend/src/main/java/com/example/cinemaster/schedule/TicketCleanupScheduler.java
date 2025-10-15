package com.example.cinemaster.scheduler;

import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketCleanupScheduler {

    private final TicketRepository ticketRepo;
    private final SeatRepository seatRepo;

    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidTickets() {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(10);
        List<Ticket> expired = ticketRepo.findByTicketStatusAndBookingTimeBefore("Pending", limit);
        for (Ticket t : expired) {
            t.setTicketStatus("Cancelled");
            ticketRepo.save(t);
            for (TicketSeat ts : t.getTicketSeats()) {
                Seat s = ts.getSeat();
                s.setStatus("Available");
                s.setLockedUntil(null);
                seatRepo.save(s);
            }
            log.info("🕐 Hủy vé quá hạn #" + t.getTicketID());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredSeatLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> locked = seatRepo.findByStatusAndLockedUntilBefore("Reserved", now);
        for (Seat s : locked) {
            s.setStatus("AVAILABLE");
            s.setLockedUntil(null);
            seatRepo.save(s);
            log.info("🔓 Giải phóng ghế " + s.getSeatNumber());
        }
    }
}
