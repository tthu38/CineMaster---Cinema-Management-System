package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    Optional<Discount> findByCode(String Code);
    boolean existsByCode(String code);
    @Query(value = """
        SELECT 
            ISNULL(SUM(td.Amount), 0) AS DiscountTotal
        FROM TicketDiscount td
        INNER JOIN Ticket t ON td.TicketID = t.TicketID
        INNER JOIN Showtimes st ON t.ShowtimeID = st.ShowtimeID
        INNER JOIN Auditorium au ON st.AuditoriumID = au.AuditoriumID
        INNER JOIN Branchs b ON au.BranchID = b.BranchID
        WHERE t.TicketStatus IN ('BOOKED', 'USED')
          AND t.BookingTime >= :from AND t.BookingTime < :to
          AND (:branchId IS NULL OR b.BranchID = :branchId)
        """, nativeQuery = true)
    BigDecimal getDiscountTotal(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") Integer branchId
    );
}
