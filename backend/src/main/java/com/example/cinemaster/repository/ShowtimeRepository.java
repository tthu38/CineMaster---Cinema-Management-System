package com.example.cinemaster.repository;

import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {

    @Query("""
        SELECT new com.example.cinemaster.dto.response.ShowtimeResponse(
            m.title,
            b.branchName,
            s.startTime,
            s.endTime,
            s.language,
            s.price
        )
        FROM Showtime s
        JOIN s.period sp
        JOIN sp.movie m
        JOIN sp.branch b
        WHERE CAST(s.startTime AS date) = CAST(GETDATE() AS date)
        ORDER BY m.title, s.startTime
    """)
    List<ShowtimeResponse> findTodayShowtimes();
}


