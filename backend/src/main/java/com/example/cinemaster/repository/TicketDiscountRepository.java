package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketDiscount;
import com.example.cinemaster.entity.TicketDiscount.TicketDiscountKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TicketDiscountRepository extends JpaRepository<TicketDiscount, TicketDiscountKey> {

    long countByDiscount_DiscountID(Integer discountId);

    long countByDiscount_DiscountIDAndTicket_Account_AccountID(Integer discountId, Integer accountId);

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

    void deleteByTicket_TicketId(Integer ticketId);


}
