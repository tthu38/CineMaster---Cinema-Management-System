package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Ticket;
import com.example.cinemaster.repository.projection.RevenueAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface RevenueRepository extends JpaRepository<Ticket, Integer> {

    @Query(value = """
        SELECT
            ISNULL(COUNT_BIG(t.TicketID), 0)            AS ticketsSold,
            ISNULL(SUM(seatData.ticketAmount), 0)       AS ticketRevenue,
            ISNULL(SUM(comboData.comboQty), 0)          AS combosSold,
            ISNULL(SUM(comboData.comboAmount), 0)       AS comboRevenue,
            ISNULL(SUM(seatData.ticketAmount) + SUM(comboData.comboAmount), 0) AS grossBeforeDiscount,
            ISNULL(SUM(discData.discountAmount), 0)     AS discountTotal,
            ISNULL(SUM(CASE WHEN t.PaymentMethod IN ('Card','Momo') THEN t.TotalPrice ELSE 0 END), 0) AS revenueOnline,
            ISNULL(SUM(CASE WHEN t.PaymentMethod = 'Cash' THEN t.TotalPrice ELSE 0 END), 0) AS revenueCash,
            ISNULL(SUM(t.TotalPrice), 0)                AS totalRevenue
        FROM Ticket AS t
            INNER JOIN Showtimes AS st   ON st.ShowtimeID = t.ShowtimeID
            INNER JOIN Auditorium AS au  ON au.AuditoriumID = st.AuditoriumID
            INNER JOIN Branchs AS b      ON b.BranchID = au.BranchID

            -- 🎟 Ghế ngồi
            LEFT JOIN (
                SELECT
                    ts.TicketID,
                    SUM(st2.Price * stype.PriceMultiplier) AS ticketAmount
                FROM TicketSeat ts
                    JOIN Ticket t2 ON t2.TicketID = ts.TicketID
                    JOIN Seat s ON s.SeatID = ts.SeatID
                    JOIN SeatType stype ON stype.TypeID = s.TypeID
                    JOIN Showtimes st2 ON st2.ShowtimeID = t2.ShowtimeID
                GROUP BY ts.TicketID
            ) AS seatData ON seatData.TicketID = t.TicketID

            -- 🍿 Combo
            LEFT JOIN (
                SELECT
                    tc.TicketID,
                    SUM(tc.Quantity) AS comboQty,
                    SUM(tc.Quantity * c.Price) AS comboAmount
                FROM TicketCombo tc
                    JOIN Combo c ON c.ComboID = tc.ComboID
                GROUP BY tc.TicketID
            ) AS comboData ON comboData.TicketID = t.TicketID

            -- 💸 Giảm giá
            LEFT JOIN (
                SELECT
                    td.TicketID,
                    SUM(td.Amount) AS discountAmount
                FROM TicketDiscount td
                GROUP BY td.TicketID
            ) AS discData ON discData.TicketID = t.TicketID

        WHERE
            t.TicketStatus IN ('BOOKED','USED')
            AND st.StartTime >= :from
            AND st.StartTime <  :to
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
