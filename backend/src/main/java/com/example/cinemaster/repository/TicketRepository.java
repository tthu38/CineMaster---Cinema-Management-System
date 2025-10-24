package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /* ======================================================
       🔹 Danh sách ghế đã được BOOKED hoặc HOLDING (chưa hết hạn)
    ====================================================== */
    @Query("""
    SELECT ts.seat.seatID
    FROM TicketSeat ts
    WHERE ts.ticket.showtime.showtimeID = :showtimeId
      AND (
          ts.ticket.ticketStatus = com.example.cinemaster.entity.Ticket.TicketStatus.BOOKED
          OR (ts.ticket.ticketStatus = com.example.cinemaster.entity.Ticket.TicketStatus.HOLDING
              AND ts.ticket.holdUntil > CURRENT_TIMESTAMP)
      )
    """)
    List<Integer> findOccupiedSeatIdsByShowtime(@Param("showtimeId") Integer showtimeId);

    // ✅ THÊM từ bài bạn: phương thức loại trừ một vé cụ thể
    @Query("""
    SELECT ts.seat.seatID
    FROM TicketSeat ts
    WHERE ts.ticket.showtime.showtimeID = :showtimeId
      AND ts.ticket.ticketStatus IN ('HOLDING', 'BOOKED')
      AND (:ticketId IS NULL OR ts.ticket.ticketId <> :ticketId)
    """)
    List<Integer> findOccupiedSeatIdsByShowtimeExcludeTicket(
            @Param("showtimeId") Integer showtimeId,
            @Param("ticketId") Integer ticketId
    );

    /* ======================================================
       🔹 Danh sách ghế đã BOOKED (dùng cho thống kê hoặc check)
    ====================================================== */
    @Query("""
        SELECT ts.seat.seatID
        FROM TicketSeat ts
        WHERE ts.ticket.showtime.showtimeID = :showtimeId
          AND ts.ticket.ticketStatus = 'BOOKED'
    """)
    List<Integer> findBookedSeatIdsByShowtime(@Param("showtimeId") Integer showtimeId);

    /* ======================================================
       🔹 Lấy danh sách vé HOLDING đã hết hạn
    ====================================================== */
    List<Ticket> findByTicketStatusAndHoldUntilBefore(
            Ticket.TicketStatus status, LocalDateTime time);

    /* ======================================================
       🔹 Lấy danh sách vé theo tài khoản (dùng cho trang “Vé của tôi”)
    ====================================================== */
    List<Ticket> findByAccount_AccountID(Integer accountId);

    List<Ticket> findByAccount_AccountIDAndTicketStatus(Integer accountId, Ticket.TicketStatus status);

    /* ======================================================
       🔹 Lấy vé có đầy đủ thông tin (dành cho chi tiết vé)
    ====================================================== */
    @EntityGraph(attributePaths = {
            "showtime",
            "showtime.period",
            "showtime.period.movie",
            "showtime.period.branch",
            "showtime.auditorium"
    })
    @Query("""
    SELECT DISTINCT t FROM Ticket t
    LEFT JOIN FETCH t.account a
    LEFT JOIN FETCH t.showtime s
    LEFT JOIN FETCH s.period p
    LEFT JOIN FETCH p.movie m
    LEFT JOIN FETCH s.auditorium au
    LEFT JOIN FETCH au.branch b
    LEFT JOIN FETCH t.ticketSeats ts
    LEFT JOIN FETCH ts.seat st
    LEFT JOIN FETCH st.seatType
    LEFT JOIN FETCH t.ticketCombos tc
    LEFT JOIN FETCH tc.combo c
    LEFT JOIN FETCH t.ticketDiscounts td
    LEFT JOIN FETCH td.discount d
    WHERE t.ticketId = :id
""")
    Optional<Ticket> findWithRelationsByTicketId(@Param("id") Integer id);

    /* ======================================================
       🔹 STAFF: Lấy danh sách vé theo chi nhánh
    ====================================================== */
    @Query("""
        SELECT t FROM Ticket t
        JOIN t.showtime s
        JOIN s.period p
        JOIN p.branch b
        WHERE b.id = :branchId
    """)
    List<Ticket> findByBranch(@Param("branchId") Integer branchId);

    /* ======================================================
       🔹 Lấy vé mới nhất theo Account
    ====================================================== */
    Optional<Ticket> findTopByAccountOrderByBookingTimeDesc(Account account);

    // ✅ THÊM từ bài bạn: lấy vé mới nhất theo account + trạng thái
    Optional<Ticket> findTopByAccount_AccountIDAndTicketStatusOrderByBookingTimeDesc(
            Integer accountId, Ticket.TicketStatus status
    );

    /* ======================================================
       🔹 Doanh thu & thống kê
    ====================================================== */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.account.accountID = :staffId
          AND t.ticketStatus = 'BOOKED'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    List<Ticket> findAllByStaffAndTimeRange(@Param("staffId") Integer staffId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(t.totalPrice), 0)
        FROM Ticket t
        WHERE t.account.accountID = :staffId
          AND t.paymentMethod = :method
          AND t.ticketStatus = 'BOOKED'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueByPaymentMethod(@Param("staffId") Integer staffId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("method") String method);

    @Query("""
        SELECT COALESCE(SUM(t.totalPrice), 0)
        FROM Ticket t
        WHERE t.account.accountID = :staffId
          AND t.paymentMethod <> :method
          AND t.ticketStatus = 'BOOKED'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueExceptPaymentMethod(@Param("staffId") Integer staffId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("method") String method);

    //------------------giang
    @Query("""
   SELECT CAST(t.bookingTime AS date), SUM(t.totalPrice)
   FROM Ticket t
   JOIN t.showtime s
   JOIN s.auditorium a
   JOIN a.branch b
   WHERE t.ticketStatus IN ('BOOKED', 'USED')
     AND CAST(t.bookingTime AS date) BETWEEN :from AND :to
     AND (:branchId IS NULL OR b.id = :branchId)
   GROUP BY CAST(t.bookingTime AS date)
   ORDER BY CAST(t.bookingTime AS date)
""")
    List<Object[]> findRevenueLast7Days(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("branchId") Integer branchId
    );

    @Query("""
   SELECT MONTH(t.bookingTime), SUM(t.totalPrice)
   FROM Ticket t
   JOIN t.showtime s
   JOIN s.auditorium a
   JOIN a.branch b
   WHERE t.ticketStatus IN ('BOOKED','USED')
     AND YEAR(t.bookingTime) = :year
     AND (:branchId IS NULL OR b.id = :branchId)
   GROUP BY MONTH(t.bookingTime)
   ORDER BY MONTH(t.bookingTime)
""")
    List<Object[]> findRevenueByMonth(@Param("year") Integer year,
                                      @Param("branchId") Integer branchId);

    @Query("""
   SELECT CAST(t.bookingTime AS date), SUM(t.totalPrice)
   FROM Ticket t
   JOIN t.showtime s
   JOIN s.auditorium a
   JOIN a.branch b
   WHERE t.ticketStatus IN ('BOOKED','USED')
     AND CAST(t.bookingTime AS date) BETWEEN :from AND :to
     AND (:branchId IS NULL OR b.id = :branchId)
   GROUP BY CAST(t.bookingTime AS date)
   ORDER BY CAST(t.bookingTime AS date)
""")
    List<Object[]> findRevenueBetweenDates(@Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("branchId") Integer branchId);

    @Query("""
   SELECT m.title, COUNT(ts.seat.seatID) AS seatsSold
   FROM TicketSeat ts
   JOIN ts.ticket t
   JOIN t.showtime s
   JOIN s.period p
   JOIN p.movie m
   JOIN s.auditorium a
   JOIN a.branch b
   WHERE t.ticketStatus IN ('BOOKED', 'USED')
     AND (:branchId IS NULL OR b.id = :branchId)
     AND (:from IS NULL OR t.bookingTime >= :from)
     AND (:to IS NULL OR t.bookingTime <= :to)
   GROUP BY m.title
   ORDER BY seatsSold DESC
""")
    List<Object[]> findTop10MoviesByTickets(
            @Param("branchId") Integer branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
