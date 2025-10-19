package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketSeatRepository extends JpaRepository<TicketSeat, Integer> {

    // 🔹 Lấy danh sách tất cả ghế theo TicketID
    List<TicketSeat> findByTicket_TicketId(Integer ticketId);

    // 🔹 (Tùy chọn) Lấy danh sách SeatID theo TicketID – dùng cho thống kê hoặc debug
    @Query("SELECT ts.seat.seatID FROM TicketSeat ts WHERE ts.ticket.ticketId = :ticketId")
    List<Integer> findSeatIdsByTicketId(@Param("ticketId") Integer ticketId);
}
