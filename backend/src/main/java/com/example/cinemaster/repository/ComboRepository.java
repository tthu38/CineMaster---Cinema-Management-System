package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Integer> {

    @Query("""
            SELECT c
            FROM Combo c
            ORDER BY CASE WHEN c.available = true THEN 0 ELSE 1 END,
                     c.id DESC
            """)
    List<Combo> findAllOrderByAvailable();

    @Query("""
            SELECT c
            FROM Combo c
            WHERE c.branchID.id = :branchId
            ORDER BY CASE WHEN c.available = true THEN 0 ELSE 1 END,
                     c.id DESC
            """)
    List<Combo> findByBranchId(@Param("branchId") Integer branchId);

    @Query("""
            SELECT c
            FROM Combo c
            WHERE c.available = true
            ORDER BY c.branchID.branchName ASC, c.id DESC
            """)
    List<Combo> findAvailableCombos();

}
