package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
       🔹 Lấy vé có đầy đủ thông tin Account + Showtime (dành cho chi tiết vé)
    ====================================================== */
    @Query("""
SELECT t FROM Ticket t
JOIN FETCH t.account a
LEFT JOIN FETCH t.showtime s
WHERE t.ticketId = :id
""")
    Optional<Ticket> findWithAccountByTicketId(@Param("id") Integer id);

    /* ======================================================
       🔹 Lấy vé mới nhất theo Account
    ====================================================== */
    Optional<Ticket> findTopByAccountOrderByBookingTimeDesc(Account account);


    /* ======================================================
       🔹 EntityGraph: Load đầy đủ quan hệ cho vé
    ====================================================== */
    @EntityGraph(attributePaths = {
            "showtime",
            "showtime.period",
            "showtime.period.movie",
            "showtime.period.branch",
            "showtime.auditorium"
    })
    Optional<Ticket> findWithRelationsByTicketId(Integer ticketID);

    /* ======================================================
       🔹 STAFF: Lấy danh sách vé theo chi nhánh
    ====================================================== */
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.showtime s " +
            "JOIN s.period p " +
            "JOIN p.branch b " +
            "WHERE b.id = :branchId")
    List<Ticket> findByBranch(@Param("branchId") Integer branchId);
}
