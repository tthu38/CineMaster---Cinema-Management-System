package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findTopByAccountOrderByBookingTimeDesc(Account account);
}
