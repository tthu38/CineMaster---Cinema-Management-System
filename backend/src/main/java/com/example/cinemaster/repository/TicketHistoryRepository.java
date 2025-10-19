package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Integer> {

    @Query("SELECT th FROM TicketHistory th WHERE th.ticket.ticketId = :ticketId ORDER BY th.changedAt ASC")
    List<TicketHistory> findByTicketIdOrdered(@Param("ticketId") Integer ticketId);
}

