package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    @Query("SELECT a FROM Account a JOIN FETCH a.role WHERE a.email = :email")
    Optional<Account> findByEmailWithRole(@Param("email") String email);

    @Query("SELECT a FROM Account a WHERE a.email = :email AND a.isActive = false ORDER BY a.createdAt DESC")
    Optional<Account> findLatestByEmail(@Param("email") String email);

    Optional<Account> findByPhoneNumber(String phoneNumber);

    @Query("SELECT a FROM Account a JOIN FETCH a.role WHERE a.phoneNumber = :phone")
    Optional<Account> findByPhoneNumberWithRole(@Param("phone") String phone);

}