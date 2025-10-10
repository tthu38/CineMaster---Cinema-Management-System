package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Showtime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Integer>, JpaSpecificationExecutor<Showtime> {

    // Lấy trong [start, end) + join đúng mapping
    @Query("""
           SELECT s FROM Showtime s
             JOIN s.period p
             JOIN p.movie m
             JOIN s.auditorium a
             JOIN a.branch b
           WHERE s.startTime >= :start
             AND s.startTime <  :end
           ORDER BY m.title, s.startTime
           """)
    List<Showtime> findTodayShowtimes(@Param("start") LocalDateTime start,
                                      @Param("end")   LocalDateTime end);

    // Overlap khi create
    @Query("""
           SELECT COUNT(s) FROM Showtime s
           WHERE s.auditorium.auditoriumID = :auditoriumId
             AND s.startTime < :endTime
             AND s.endTime   > :startTime
           """)
    long countOverlaps(@Param("auditoriumId") Integer auditoriumId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    // Overlap khi update (loại trừ chính nó)
    @Query("""
           SELECT COUNT(s) FROM Showtime s
           WHERE s.auditorium.auditoriumID = :auditoriumId
             AND s.startTime < :endTime
             AND s.endTime   > :startTime
             AND s.showtimeID <> :excludeId
           """)
    long countOverlapsExcluding(@Param("auditoriumId") Integer auditoriumId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeId") Integer excludeId);

    // [start, end) cho tất cả chi nhánh
    List<Showtime> findAllByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime start,
                                                                          LocalDateTime end);

    List<Showtime> findAllByStartTimeGreaterThanEqualAndStartTimeLessThanAndAuditorium_Branch_Id(
            LocalDateTime start,
            LocalDateTime end,
            Integer branchId
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
   SELECT s FROM Showtime s
   WHERE s.auditorium.auditoriumID = :auditoriumId
     AND s.startTime < :endTime
     AND s.endTime   > :startTime
""")
    List<Showtime> findOverlapsForUpdate(@Param("auditoriumId") Integer auditoriumId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("""
       select s from Showtime s
       where s.auditorium.auditoriumID = :audId
         and s.startTime < :endWithBuffer
         and s.endTime   > :startWithBuffer
       """)
    List<Showtime> findOverlapsWithBufferForUpdate(
            @Param("audId") Integer auditoriumId,
            @Param("startWithBuffer") LocalDateTime startWithBuffer,
            @Param("endWithBuffer") LocalDateTime endWithBuffer);

    // Bản cho update (loại trừ chính nó)
    @Query("""
       select count(s) from Showtime s
       where s.auditorium.auditoriumID = :audId
         and s.startTime < :endWithBuffer
         and s.endTime   > :startWithBuffer
         and s.showtimeID <> :excludeId
       """)
    long countOverlapsWithBufferExcluding(
            @Param("audId") Integer auditoriumId,
            @Param("startWithBuffer") LocalDateTime startWithBuffer,
            @Param("endWithBuffer") LocalDateTime endWithBuffer,
            @Param("excludeId") Integer excludeId);

    @Query("""
        select count(s) from Showtime s
        where s.period.movie.movieID = :movieId
          and s.auditorium.branch.id = :branchId
          and s.startTime < :end
          and s.endTime   > :start
    """)
    long countMovieOverlapInBranch(@Param("movieId") Integer movieId,
                                   @Param("branchId") Integer branchId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
        select count(s) from Showtime s
        where s.period.movie.movieID = :movieId
          and s.auditorium.branch.id = :branchId
          and s.startTime < :end
          and s.endTime   > :start
          and s.showtimeID <> :excludeId
    """)
    long countMovieOverlapInBranchExcluding(@Param("movieId") Integer movieId,
                                            @Param("branchId") Integer branchId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("excludeId") Integer excludeId);
}
