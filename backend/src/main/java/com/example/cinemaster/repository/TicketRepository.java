package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findAllByAccount_AccountID(Integer accountId);
}
