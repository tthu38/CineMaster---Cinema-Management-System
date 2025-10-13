package com.example.cinemaster.repository;

import com.example.cinemaster.entity.WorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;

public interface WorkHistoryRepository
        extends JpaRepository<WorkHistory, Integer>, JpaSpecificationExecutor<WorkHistory> {

    boolean existsByAccountID_AccountIDAndActionAndActionTimeBetween(
            Integer accountId, String action, Instant from, Instant to);

    boolean existsByAccountID_AccountIDAndActionAndIdNotAndActionTimeBetween(
            Integer accountId, String action, Integer excludeId, Instant from, Instant to);
}

