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

    // 🔹 Lấy danh sách ghế theo phòng và dãy
    List<Seat> findAllByAuditoriumAuditoriumIDAndSeatRow(Integer auditoriumID, String seatRow);

    // 🔹 Lấy tất cả ghế trong 1 phòng
    List<Seat> findAllByAuditorium_AuditoriumID(Integer auditoriumId);

    // 🔹 Kiểm tra trùng số ghế trong cùng phòng (dùng khi create seat)
    boolean existsByAuditoriumAuditoriumIDAndSeatNumber(Integer auditoriumID, String seatNumber);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatID = :seatId")
    Seat lockSeatForBooking(@Param("seatId") Integer seatId);


}
