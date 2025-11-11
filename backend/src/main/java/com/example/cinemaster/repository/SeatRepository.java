package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findAllByAuditoriumAuditoriumIDAndSeatRow(Integer auditoriumID, String seatRow);

    List<Seat> findAllByAuditorium_AuditoriumID(Integer auditoriumId);

    boolean existsByAuditoriumAuditoriumIDAndSeatNumber(Integer auditoriumID, String seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatID = :seatId")
    Seat lockSeatForBooking(@Param("seatId") Integer seatId);

    List<Seat> findByAuditorium_AuditoriumID(Integer auditoriumId);

}
