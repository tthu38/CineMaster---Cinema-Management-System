package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketCombo;
import com.example.cinemaster.entity.TicketCombo.TicketComboKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketComboRepository extends JpaRepository<TicketCombo, TicketComboKey> {
    List<TicketCombo> findByTicket_TicketID(Integer ticketId);
}
