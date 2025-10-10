// src/main/java/com/example/cinemaster/repository/ScreeningPeriodRepository.java
package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ScreeningPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScreeningPeriodRepository extends JpaRepository<ScreeningPeriod, Integer> {

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
}
