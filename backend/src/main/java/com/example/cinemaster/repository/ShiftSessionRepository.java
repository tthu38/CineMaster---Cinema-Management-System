package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShiftSessionRepository extends JpaRepository<ShiftSession, Integer> {

    @Query("""
    SELECT s FROM ShiftSession s
    WHERE s.staff.id = :staffId AND s.status = 'OPEN'
    ORDER BY s.startTime DESC
""")
    List<ShiftSession> findActiveSessions(@Param("staffId") Integer staffId);

    default Optional<ShiftSession> findActiveSession(Integer staffId) {
        var list = findActiveSessions(staffId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}

