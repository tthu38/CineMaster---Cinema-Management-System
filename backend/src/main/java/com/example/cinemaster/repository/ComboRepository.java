package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Integer> {

    @Query("""
            SELECT c
            FROM Combo c
            ORDER BY CASE WHEN c.available = true THEN 0 ELSE 1 END,
                     c.id DESC
            """)
    List<Combo> findAllOrderByAvailable();

    @Query("""
            SELECT c
            FROM Combo c
            WHERE c.branchID.id = :branchId
            ORDER BY CASE WHEN c.available = true THEN 0 ELSE 1 END,
                     c.id DESC
            """)
    List<Combo> findByBranchId(@Param("branchId") Integer branchId);

    @Query("""
            SELECT c
            FROM Combo c
            WHERE c.available = true
            ORDER BY c.branchID.branchName ASC, c.id DESC
            """)
    List<Combo> findAvailableCombos();
    @Query("""
       SELECT c FROM Combo c
       WHERE (c.branchID.id = :branchId OR c.branchID IS NULL)
       AND c.available = true
       ORDER BY CASE WHEN c.branchID IS NULL THEN 0 ELSE 1 END, c.id DESC
       """)
    List<Combo> findAvailableByBranchIncludingGlobal(Integer branchId);
    @Query(value = """
    SELECT ISNULL(SUM(tc.Quantity), 0)
    FROM TicketCombo tc
    JOIN Ticket t ON t.TicketID = tc.TicketID
    JOIN Showtimes sh ON sh.ShowtimeID = t.ShowtimeID
    JOIN Auditorium a ON a.AuditoriumID = sh.AuditoriumID
    JOIN Branchs b ON b.BranchID = a.BranchID
    WHERE t.TicketStatus IN ('BOOKED','USED')
      AND t.BookingTime >= :from AND t.BookingTime < :to
      AND (:branchId IS NULL OR b.BranchID = :branchId)
    """, nativeQuery = true)
    Long countCombosSold(@Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("branchId") Integer branchId);

}
