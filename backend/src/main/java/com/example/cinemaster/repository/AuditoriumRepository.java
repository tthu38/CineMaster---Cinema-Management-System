// src/main/java/com/example/cinemaster/repository/AuditoriumRepository.java
package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Integer> {

    @Query("""
        SELECT a FROM Auditorium a
        WHERE a.branch.id = :branchId
          AND (a.isActive = true OR a.isActive IS NULL)
    """)
    List<Auditorium> findActiveByBranch(@Param("branchId") Integer branchId);
}

