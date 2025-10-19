package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TicketComboRepository extends JpaRepository<TicketCombo, Integer> {

    // 🔹 Tổng số combo bán được
    @Query("""
        SELECT COALESCE(SUM(c.quantity), 0)
        FROM TicketCombo c
        WHERE c.ticket.account.id = :staffId
          AND c.ticket.ticketStatus = 'Booked'
          AND c.ticket.bookingTime BETWEEN :from AND :to
    """)
    int countCombosByStaffAndTime(@Param("staffId") Integer staffId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    // 🔹 Tổng doanh thu combo (dựa theo combo.price * quantity)
    @Query("""
        SELECT COALESCE(SUM(c.combo.price * c.quantity), 0)
        FROM TicketCombo c
        WHERE c.ticket.account.id = :staffId
          AND c.ticket.ticketStatus = 'Booked'
          AND c.ticket.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumComboRevenueByStaffAndTime(@Param("staffId") Integer staffId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
