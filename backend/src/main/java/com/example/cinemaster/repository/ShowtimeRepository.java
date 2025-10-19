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

    /* ============================================================
       📅 LỊCH CHIẾU THEO NGÀY / TUẦN
    ============================================================ */
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
                                      @Param("end") LocalDateTime end);

    // ✅ Dành cho Admin xem tất cả chi nhánh
    List<Showtime> findAllByStartTimeGreaterThanEqualAndStartTimeLessThan(
            LocalDateTime start, LocalDateTime end);

    // ✅ Dành cho Manager xem theo chi nhánh
    @Query("""
        SELECT s FROM Showtime s
        WHERE s.startTime >= :start
          AND s.startTime < :end
          AND s.auditorium.branch.id = :branchId
        ORDER BY s.startTime
    """)
    List<Showtime> findWeekByBranch(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("branchId") Integer branchId);

    /* ============================================================
       ⚙️ KIỂM TRA TRÙNG SUẤT CHIẾU
    ============================================================ */
    @Query("""
       SELECT COUNT(s) FROM Showtime s
       WHERE s.auditorium.auditoriumID = :auditoriumId
         AND NOT ( s.endTime <= :startWithBuffer OR s.startTime >= :endWithBuffer )
    """)
    long countOverlaps(@Param("auditoriumId") Integer auditoriumId,
                       @Param("startWithBuffer") LocalDateTime startWithBuffer,
                       @Param("endWithBuffer") LocalDateTime endWithBuffer);

    @Query("""
       SELECT COUNT(s) FROM Showtime s
       WHERE s.auditorium.auditoriumID = :auditoriumId
         AND NOT ( s.endTime <= :startWithBuffer OR s.startTime >= :endWithBuffer )
         AND s.showtimeID <> :excludeId
    """)
    long countOverlapsExcluding(@Param("auditoriumId") Integer auditoriumId,
                                @Param("startWithBuffer") LocalDateTime startWithBuffer,
                                @Param("endWithBuffer") LocalDateTime endWithBuffer,
                                @Param("excludeId") Integer excludeId);

    /* ============================================================
       🎬 TRÙNG PHIM TRONG CÙNG PHÒNG (CHI NHÁNH)
    ============================================================ */
    @Query("""
    SELECT COUNT(s) FROM Showtime s
    WHERE s.period.movie.movieID = :movieId
      AND s.auditorium.branch.id = :branchId
      AND s.auditorium.auditoriumID = :auditoriumId
      AND s.startTime < :end
      AND s.endTime   > :start
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
      AND s.showtimeID <> :excludeId
""")
    long countMovieOverlapInBranchExcluding(@Param("movieId") Integer movieId,
                                            @Param("branchId") Integer branchId,
                                            @Param("auditoriumId") Integer auditoriumId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("excludeId") Integer excludeId);

    /* ============================================================
       🔐 KHÓA PESSIMISTIC (TRÁNH RACE CONDITION)
    ============================================================ */
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

    /* ============================================================
       🔍 KHÁC
    ============================================================ */
    @Query("""
           SELECT s FROM Showtime s
           JOIN s.period p
           JOIN p.branch b
           WHERE b.id = :branchId
           AND CAST(s.startTime AS date) = :date
           ORDER BY s.startTime ASC
           """)
    List<Showtime> findByBranchIdAndDate(@Param("branchId") Integer branchId,
                                         @Param("date") LocalDate date);
}
