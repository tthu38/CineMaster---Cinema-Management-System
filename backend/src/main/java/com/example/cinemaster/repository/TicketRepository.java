package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    List<Ticket> findByAccount_AccountID(Integer accountID);

    @EntityGraph(attributePaths = {
            "showtime",
            "showtime.period",
            "showtime.period.movie",
            "showtime.period.branch",
            "showtime.auditorium"
    })
    Optional<Ticket> findWithRelationsByTicketID(Integer ticketID);

    // ✅ Thêm mới: Staff lấy vé theo chi nhánh
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.showtime s " +
            "JOIN s.period p " +
            "JOIN p.branch b " +
            "WHERE b.id = :branchId")
    List<Ticket> findByBranch(Integer branchId);
}
