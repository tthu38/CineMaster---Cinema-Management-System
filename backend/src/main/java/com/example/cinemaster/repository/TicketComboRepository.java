package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketCombo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketComboRepository extends JpaRepository<TicketCombo, Integer> {
    @EntityGraph(attributePaths = {"combo"})
    List<TicketCombo> findByTicket_TicketId(Integer ticketId);
}
