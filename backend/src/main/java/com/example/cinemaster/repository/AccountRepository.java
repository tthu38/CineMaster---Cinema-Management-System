package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<Account> findByIsActiveTrue();

    Page<Account> findByRole_Id(Integer roleId, Pageable pageable);

    // Lọc theo branch
    Page<Account> findByBranch_Id(Integer branchId, Pageable pageable);

    // Search (email, tên, phone)
    @Query("SELECT a FROM Account a " +
            "WHERE (:keyword IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR a.phoneNumber LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:roleId IS NULL OR a.role.id = :roleId) " +
            "AND (:branchId IS NULL OR a.branch.id = :branchId)")
    Page<Account> searchAccounts(@Param("keyword") String keyword,
                                 @Param("roleId") Integer roleId,
                                 @Param("branchId") Integer branchId,
                                 Pageable pageable);
}
