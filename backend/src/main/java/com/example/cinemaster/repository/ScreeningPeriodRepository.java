package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ScreeningPeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScreeningPeriodRepository extends JpaRepository<ScreeningPeriod, Integer> {

    @EntityGraph(attributePaths = {"movie", "branch"})
    @Override
    List<ScreeningPeriod> findAll();

    @EntityGraph(attributePaths = {"movie", "branch"})
    List<ScreeningPeriod> findByBranch_Id(Integer branchId);

    @Query("""
        SELECT p FROM ScreeningPeriod p
        WHERE (:branchId IS NULL OR p.branch.id = :branchId)          
          AND (:from IS NULL OR p.endDate   >= :from)
          AND (:to   IS NULL OR p.startDate <= :to)
          AND (p.isActive = true)                                      
        ORDER BY p.startDate
    """)
    List<ScreeningPeriod> findActive(@Param("branchId") Integer branchId,
                                     @Param("from") LocalDate from,
                                     @Param("to")   LocalDate to);

    // Bạn có thể thêm các tùy chỉnh khác nếu cần, ví dụ:
    // boolean existsByMovie_MovieIDAndBranch_Id(Integer movieID, Integer branchID);
}
