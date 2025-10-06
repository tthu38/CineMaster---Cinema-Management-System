package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Integer> {

    @Query("SELECT c FROM Combo c ORDER BY CASE WHEN c.available = true THEN 0 ELSE 1 END")
    List<Combo> findAllOrderByAvailable();

}
