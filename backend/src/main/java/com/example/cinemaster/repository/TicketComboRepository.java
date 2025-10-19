package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketComboRepository extends JpaRepository<TicketCombo, TicketCombo.TicketComboKey> {
    List<TicketCombo> findByTicket_TicketID(Integer ticketId);
}
