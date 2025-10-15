package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketSeat;
import com.example.cinemaster.entity.TicketSeatId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSeatRepository extends JpaRepository<TicketSeat, TicketSeatId> {
    List<TicketSeat> findAllByTicket_TicketID(Integer ticketId);
}
