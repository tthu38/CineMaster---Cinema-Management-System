package com.example.cinemaster.repository;

import com.example.cinemaster.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Integer> {
    boolean existsByLevelNameIgnoreCase(String levelName);
    boolean existsByLevelNameIgnoreCaseAndIdNot(String levelName, Integer id);
}
