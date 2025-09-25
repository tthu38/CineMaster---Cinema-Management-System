package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);

    @Query("SELECT a FROM Account a JOIN FETCH a.role WHERE a.email = :email")
    Optional<Account> findByEmailWithRole(String email);

    Optional<Account> findByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT TOP 1 * FROM Accounts WHERE Email = :email ORDER BY CreatedAt DESC", nativeQuery = true)
    Optional<Account> findLatestByEmail(@Param("email") String email);

    Optional<Account> findByEmail(String email);

}
