package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Integer> {

    @Query("""
        SELECT o FROM Otp o
        WHERE o.code = :code
        AND o.expiry > :now
    """)
    Optional<Otp> findValidOtp(@Param("code") String code, @Param("now") LocalDateTime now);
}
