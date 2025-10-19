package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    // 🔹 Lấy tất cả vé bán bởi nhân viên trong khoảng thời gian
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.account.id = :staffId
          AND t.ticketStatus = 'Booked'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    List<Ticket> findAllByStaffAndTimeRange(@Param("staffId") Integer staffId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    // 🔹 Tổng doanh thu theo phương thức thanh toán
    @Query("""
        SELECT COALESCE(SUM(t.totalPrice), 0)
        FROM Ticket t
        WHERE t.account.id = :staffId
          AND t.paymentMethod = :method
          AND t.ticketStatus = 'Booked'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueByPaymentMethod(@Param("staffId") Integer staffId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("method") String method);

    // 🔹 Tổng doanh thu ngoại trừ một phương thức (VD: không tính CASH)
    @Query("""
        SELECT COALESCE(SUM(t.totalPrice), 0)
        FROM Ticket t
        WHERE t.account.id = :staffId
          AND t.paymentMethod <> :method
          AND t.ticketStatus = 'Booked'
          AND t.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueExceptPaymentMethod(@Param("staffId") Integer staffId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("method") String method);
}
