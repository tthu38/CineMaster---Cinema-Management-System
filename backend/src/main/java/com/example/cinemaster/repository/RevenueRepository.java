package com.example.cinemaster.repository;


import com.example.cinemaster.entity.Payment;
import com.example.cinemaster.repository.projection.RevenueAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface RevenueRepository extends JpaRepository<Payment, Integer> {


    // ✅ Admin: tất cả chi nhánh trong khoảng thời gian
    @Query("""
       SELECT p FROM Payment p
       WHERE p.status = 'SUCCESS'
       AND p.createdAt BETWEEN :from AND :to
   """)
    List<Payment> findAllInRange(@Param("from") Instant from, @Param("to") Instant to);


    // ✅ Manager: theo chi nhánh
    @Query("""
       SELECT p FROM Payment p
       JOIN p.ticketID t
       JOIN t.showtime s
       JOIN s.auditorium a
       WHERE a.branch.id = :branchId
       AND p.status = 'SUCCESS'
       AND p.createdAt BETWEEN :from AND :to
   """)
    List<Payment> findAllByBranchInRange(
            @Param("branchId") Integer branchId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );


    // ✅ Staff: trong ngày cụ thể
    @Query("""
       SELECT p FROM Payment p
       JOIN p.ticketID t
       JOIN t.showtime s
       JOIN s.auditorium a
       WHERE a.branch.id = :branchId
       AND p.status = 'SUCCESS'
       AND CAST(p.createdAt AS date) = :date
   """)
    List<Payment> findAllByBranchAndDate(
            @Param("branchId") Integer branchId,
            @Param("date") LocalDate date
    );


    // ✅ Thống kê doanh thu chi tiết (theo công thức mới)
    @Query(value = """
       SELECT
           ISNULL(SUM(seatData.seatCount), 0) AS ticketsSold,
           ISNULL(SUM(seatData.ticketAmount), 0) AS ticketRevenue,
           ISNULL(SUM(comboData.comboQty), 0) AS combosSold,
           ISNULL(SUM(comboData.comboAmount), 0) AS comboRevenue,
           ISNULL(SUM(discData.discountAmount), 0) AS discountTotal,
           ISNULL(SUM(CASE WHEN t.PaymentMethod IN ('Card','Momo') THEN t.TotalPrice ELSE 0 END), 0) AS revenueOnline,
           ISNULL(SUM(CASE WHEN t.PaymentMethod = 'Cash' THEN t.TotalPrice ELSE 0 END), 0) AS revenueCash,
           ISNULL(SUM(t.TotalPrice), 0) AS totalRevenue
       FROM Ticket AS t
           INNER JOIN Showtimes AS st   ON st.ShowtimeID = t.ShowtimeID
           INNER JOIN Auditorium AS au  ON au.AuditoriumID = st.AuditoriumID
           INNER JOIN Branchs AS b      ON b.BranchID = au.BranchID


           -- 🪑 Subquery: ghế
           LEFT JOIN (
               SELECT
                   ts.TicketID,
                   COUNT(ts.SeatID) AS seatCount,
                   SUM(st2.Price * stype.PriceMultiplier) AS ticketAmount
               FROM TicketSeat ts
                   JOIN Ticket t2 ON t2.TicketID = ts.TicketID
                   JOIN Seat s ON s.SeatID = ts.SeatID
                   JOIN SeatType stype ON stype.TypeID = s.TypeID
                   JOIN Showtimes st2 ON st2.ShowtimeID = t2.ShowtimeID
               GROUP BY ts.TicketID
           ) AS seatData ON seatData.TicketID = t.TicketID


           -- 🍿 Subquery: combo
           LEFT JOIN (
               SELECT
                   tc.TicketID,
                   SUM(tc.Quantity) AS comboQty,
                   SUM(tc.Quantity * c.Price) AS comboAmount
               FROM TicketCombo tc
                   JOIN Combo c ON c.ComboID = tc.ComboID
               GROUP BY tc.TicketID
           ) AS comboData ON comboData.TicketID = t.TicketID


           -- 💸 Subquery: giảm giá
           LEFT JOIN (
               SELECT
                   td.TicketID,
                   SUM(td.Amount) AS discountAmount
               FROM TicketDiscount td
               GROUP BY td.TicketID
           ) AS discData ON discData.TicketID = t.TicketID


       WHERE
           t.TicketStatus IN ('BOOKED','USED')
           AND t.BookingTime >= :from
           AND t.BookingTime < :to
           AND (
               :branchId IS NULL
               OR b.BranchID = CAST(:branchId AS INT)
           )
       """, nativeQuery = true)
    RevenueAggregate aggregateForWindow(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") Integer branchId
    );


}

