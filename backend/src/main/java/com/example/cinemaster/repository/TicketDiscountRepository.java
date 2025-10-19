package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketDiscount;
import com.example.cinemaster.entity.TicketDiscount.TicketDiscountKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TicketDiscountRepository extends JpaRepository<TicketDiscount, TicketDiscountKey> {

    // 🔹 Đếm tổng số lần sử dụng discount
    long countByDiscount_DiscountID(Integer discountId);

    // 🔹 Đếm số lần một account dùng discount
    long countByDiscount_DiscountIDAndTicket_Account_AccountID(Integer discountId, Integer accountId);

    // 🔹 Đếm số lần dùng discount trong ngày của account
    @Query("""
        SELECT COUNT(td) 
        FROM TicketDiscount td 
        WHERE td.discount.discountID = :discountId 
          AND td.ticket.account.accountID = :accountId 
          AND td.ticket.bookingTime BETWEEN :startOfDay AND :endOfDay
    """)
    long countDailyUsage(@Param("discountId") Integer discountId,
                         @Param("accountId") Integer accountId,
                         @Param("startOfDay") LocalDateTime startOfDay,
                         @Param("endOfDay") LocalDateTime endOfDay);

    // 🔹 Xóa tất cả discount liên kết với 1 vé
    void deleteByTicket_TicketId(Integer ticketId);

    // 🔹 Tổng số tiền đã giảm trong ca làm của nhân viên
    @Query("""
        SELECT COALESCE(SUM(td.amount), 0)
        FROM TicketDiscount td
        WHERE td.ticket.account.accountID = :staffId
          AND td.ticket.ticketStatus = 'Booked'
          AND td.ticket.bookingTime BETWEEN :from AND :to
    """)
    BigDecimal sumDiscountByStaffAndTime(@Param("staffId") Integer staffId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
