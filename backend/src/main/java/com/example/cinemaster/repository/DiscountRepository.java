package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    Optional<Discount> findByCode(String Code);
    boolean existsByCode(String code);


}
