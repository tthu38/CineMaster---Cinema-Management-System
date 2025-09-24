package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT a FROM Account a JOIN FETCH a.role WHERE a.email = :email")
    Optional<Account> findByEmailWithRole(String email);

    Optional<Account> findByPhoneNumber(String phoneNumber);

    @Query("SELECT a FROM Account a WHERE a.email = :email ORDER BY a.createdAt DESC LIMIT 1")
    Optional<Account> findLatestByEmail(String email);
}
