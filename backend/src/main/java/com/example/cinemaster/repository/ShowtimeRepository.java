package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Showtime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer>, JpaSpecificationExecutor<Showtime> {

    @Query("""
          SELECT s FROM Showtime s
            JOIN s.period p
            JOIN p.movie m
            JOIN s.auditorium a
            JOIN a.branch b
          WHERE s.startTime >= :start
            AND s.startTime <  :end
            AND s.status = 'ACTIVE'
          ORDER BY m.title, s.startTime
          """)
    List<Showtime> findTodayShowtimes(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    List<Showtime> findAllByStartTimeGreaterThanEqualAndStartTimeLessThanAndStatus(
            LocalDateTime start, LocalDateTime end, String status);

    @Query("""
       SELECT s FROM Showtime s
       WHERE s.startTime >= :start
         AND s.startTime < :end
         AND s.auditorium.branch.id = :branchId
         AND s.status = 'ACTIVE'
       ORDER BY s.startTime
   """)
    List<Showtime> findWeekByBranch(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("branchId") Integer branchId);


    @Query("""
SELECT COUNT(s)
FROM Showtime s
WHERE s.auditorium.auditoriumID = :auditoriumId
  AND s.auditorium.branch.id = :branchId
  AND s.status = 'ACTIVE'
  AND (
       s.endTime > :startMinusBuffer
       AND s.startTime < :end
  )
""")
    long countOverlaps(@Param("branchId") Integer branchId,
                       @Param("auditoriumId") Integer auditoriumId,
                       @Param("startMinusBuffer") LocalDateTime startMinusBuffer,
                       @Param("end") LocalDateTime end);


    @Query("""
SELECT COUNT(s)
FROM Showtime s
WHERE s.auditorium.auditoriumID = :auditoriumId
  AND s.auditorium.branch.id = :branchId
  AND s.status = 'ACTIVE'
  AND s.showtimeID <> :excludeId
  AND (
       s.endTime > :startMinusBuffer
       AND s.startTime < :end
  )
""")
    long countOverlapsExcluding(@Param("branchId") Integer branchId,
                                @Param("auditoriumId") Integer auditoriumId,
                                @Param("startMinusBuffer") LocalDateTime startMinusBuffer,
                                @Param("end") LocalDateTime end,
                                @Param("excludeId") Integer excludeId);


    @Query("""
   SELECT COUNT(s) FROM Showtime s
   WHERE s.period.movie.movieID = :movieId
     AND s.auditorium.branch.id = :branchId
     AND s.auditorium.auditoriumID = :auditoriumId
     AND s.startTime < :end
     AND s.endTime   > :start
     AND s.status = 'ACTIVE'
""")
    long countMovieOverlapInBranch(@Param("movieId") Integer movieId,
                                   @Param("branchId") Integer branchId,
                                   @Param("auditoriumId") Integer auditoriumId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
   SELECT COUNT(s) FROM Showtime s
   WHERE s.period.movie.movieID = :movieId
     AND s.auditorium.branch.id = :branchId
     AND s.auditorium.auditoriumID = :auditoriumId
     AND s.startTime < :end
     AND s.endTime   > :start
     AND s.status = 'ACTIVE'
     AND s.showtimeID <> :excludeId
""")
    long countMovieOverlapInBranchExcluding(@Param("movieId") Integer movieId,
                                            @Param("branchId") Integer branchId,
                                            @Param("auditoriumId") Integer auditoriumId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("excludeId") Integer excludeId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      SELECT s FROM Showtime s
      WHERE s.auditorium.auditoriumID = :auditoriumId
        AND s.startTime < :endTime
        AND s.endTime   > :startTime
        AND s.status = 'ACTIVE'
   """)
    List<Showtime> findOverlapsForUpdate(@Param("auditoriumId") Integer auditoriumId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);


    @Query("""
          SELECT s FROM Showtime s
          JOIN s.period p
          JOIN p.branch b
          WHERE b.id = :branchId
          AND CAST(s.startTime AS date) = :date
          AND s.status = 'ACTIVE'
          ORDER BY s.startTime ASC
          """)
    List<Showtime> findByBranchIdAndDate(@Param("branchId") Integer branchId,
                                         @Param("date") LocalDate date);

    @Query("""
   SELECT s FROM Showtime s
   JOIN s.period p
   JOIN p.movie m
   JOIN s.auditorium a
   JOIN a.branch b
   WHERE b.id = :branchId
     AND m.movieID = :movieId
     AND s.startTime >= :from
     AND s.startTime < :to
     AND s.status = 'ACTIVE'
   ORDER BY s.startTime
""")
    List<Showtime> findByBranchAndMovieInRange(@Param("branchId") Integer branchId,
                                               @Param("movieId") Integer movieId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Query("""
   SELECT s FROM Showtime s
   JOIN s.period p
   JOIN p.movie m
   WHERE m.movieID = :movieId
     AND s.startTime >= :from
     AND s.startTime < :to
     AND s.status = 'ACTIVE'
   ORDER BY s.startTime
""")
    List<Showtime> findByMovieInRange(@Param("movieId") Integer movieId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);


    //Thư
    @Query("SELECT s FROM Showtime s WHERE LOWER(s.auditorium.branch.branchName) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Showtime> findByAuditorium_Branch_BranchNameContainingIgnoreCaseAndStatus(@Param("city") String city, @Param("status") String status);




    @Query("""
    SELECT s FROM Showtime s
    WHERE s.startTime >= :from AND s.startTime < :to
      AND s.status = :status
      AND (:branchId IS NULL OR s.auditorium.branch.id = :branchId)
      AND (:movieId IS NULL OR s.period.movie.movieID = :movieId)
    ORDER BY s.startTime ASC
""")
    List<Showtime> findAllByFilter(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") Integer branchId,
            @Param("movieId") Integer movieId,
            @Param("status") String status
    );
}
