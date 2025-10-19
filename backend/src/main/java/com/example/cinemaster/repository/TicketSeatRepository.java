package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketSeatRepository extends JpaRepository<TicketSeat, TicketSeat.TicketSeatKey> {
    List<TicketSeat> findByTicket_TicketID(Integer ticketId);
}
