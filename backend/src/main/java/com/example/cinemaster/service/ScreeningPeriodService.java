package com.example.cinemaster.service;

import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.repository.ScreeningPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreeningPeriodService {
    private final ScreeningPeriodRepository repo;

    public List<ScreeningPeriod> findActive(Integer branchId, LocalDate onDate) {
        // Trả về tất cả period đang bao phủ ngày onDate
        return repo.findActive(branchId, onDate, onDate);
    }

    public ScreeningPeriod getById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("ScreeningPeriod not found: " + id));
    }
}

