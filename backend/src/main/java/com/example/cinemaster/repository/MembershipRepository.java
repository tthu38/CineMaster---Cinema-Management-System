package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {

    // Fetch đầy đủ cả Level + Account để tránh lazy-load lỗi
    @Query("""
    SELECT m FROM Membership m
    JOIN FETCH m.account a
    LEFT JOIN FETCH m.level l
    WHERE a.accountID = :accountId
    """)
    Optional<Membership> findByAccount_AccountID(@Param("accountId") Integer accountId);

    Optional<Membership> findByAccount(Account account);

    boolean existsByAccount_AccountID(Integer accountId);

    Optional<Membership> findByAccount_AccountIDAndExpiryDateAfter(Integer accountId, LocalDate today);
}
