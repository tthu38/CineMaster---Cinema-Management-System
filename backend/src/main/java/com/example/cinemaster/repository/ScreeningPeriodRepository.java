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

    // 🟢 Load tất cả, kèm movie & branch (tránh N+1)
    @EntityGraph(attributePaths = {"movie", "branch"})
    @Override
    List<ScreeningPeriod> findAll();

    // 🟢 Tìm theo chi nhánh
    @EntityGraph(attributePaths = {"movie", "branch"})
    List<ScreeningPeriod> findByBranch_Id(Integer branchId);

    // 🟢 Tìm các kỳ chiếu đang hoạt động theo ngày
    @EntityGraph(attributePaths = {"movie", "branch"})
    @Query("""
        SELECT p FROM ScreeningPeriod p
        WHERE (:branchId IS NULL OR p.branch.id = :branchId)
          AND (
                :onDate IS NULL
                OR (p.startDate <= :onDate AND p.endDate >= :onDate)
              )
          AND (p.isActive IS NULL OR p.isActive = TRUE)
        ORDER BY p.startDate
    """)
    List<ScreeningPeriod> findActive(
            @Param("branchId") Integer branchId,
            @Param("onDate") LocalDate onDate
    );
}
