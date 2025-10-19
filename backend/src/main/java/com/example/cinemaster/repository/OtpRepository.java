package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Integer> {
}
