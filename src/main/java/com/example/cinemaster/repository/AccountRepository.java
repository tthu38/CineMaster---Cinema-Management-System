package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Account> findByAccountID(Integer accountID);
}