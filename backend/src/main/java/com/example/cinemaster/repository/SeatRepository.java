package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    // Tự động kế thừa các phương thức CRUD cơ bản (save, findById, findAll, deleteById, existsById)
    List<Seat> findAllByAuditoriumAuditoriumIDAndSeatRow(Integer auditoriumID, String seatRow);
    List<Seat> findByAuditorium_AuditoriumID(Integer auditoriumId);
}
