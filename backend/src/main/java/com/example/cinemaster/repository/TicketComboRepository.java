package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketCombo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketComboRepository extends JpaRepository<TicketCombo, Integer> {

    boolean existsByTicket_TicketIdAndCombo_Id(Integer ticketId, Integer comboId);


    @EntityGraph(attributePaths = {"combo"})
    List<TicketCombo> findByTicket_TicketId(Integer ticketId);

    @Query("""
        SELECT COALESCE(SUM(c.quantity), 0)
        FROM TicketCombo c
        WHERE c.ticket.account.accountID = :staffId
          AND c.ticket.ticketStatus = 'BOOKED'
          AND c.ticket.bookingTime BETWEEN :from AND :to
    """)
    int countCombosByStaffAndTime(@Param("staffId") Integer staffId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(c.combo.price * c.quantity), 0)
        FROM TicketCombo c
        WHERE c.ticket.account.accountID = :staffId
          AND c.ticket.ticketStatus = 'BOOKED'
          AND c.ticket.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumComboRevenueByStaffAndTime(@Param("staffId") Integer staffId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
