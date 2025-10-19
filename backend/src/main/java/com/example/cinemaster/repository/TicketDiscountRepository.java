package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TicketDiscountRepository extends JpaRepository<TicketDiscount, Integer> {

    // 🔹 Tổng số tiền đã giảm trong ca làm
    @Query("""
        SELECT COALESCE(SUM(d.amount), 0)
        FROM TicketDiscount d
        WHERE d.ticket.account.id = :staffId
          AND d.ticket.ticketStatus = 'Booked'
          AND d.ticket.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumDiscountByStaffAndTime(@Param("staffId") Integer staffId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
