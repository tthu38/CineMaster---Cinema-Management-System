package com.example.cinemaster.repository;

import com.example.cinemaster.dto.response.StaffSimpleResponse;
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

    @Query("""
       select new com.example.cinemaster.dto.response.StaffSimpleResponse(a.accountID, a.fullName)
         from Account a
       where a.role.roleName = 'Staff' and (a.isActive = true or a.isActive is null)
       order by a.fullName asc
       """)
    List<StaffSimpleResponse> findAllStaffSimple();
    @Query("""
       SELECT a
       FROM Account a
       JOIN a.branch b        
       WHERE a.role.id = 3
         AND b.id = :branchId
    """)
    List<Account> findStaffsByBranch(@Param("branchId") Integer branchId);

    @Query("""
    SELECT a
    FROM Account a
    WHERE (:keyword IS NULL OR a.fullName LIKE %:keyword% OR a.email LIKE %:keyword%)
      AND (:roleId IS NULL OR a.role.id = :roleId)
      AND (:branchId IS NULL OR a.branch.id = :branchId)
      AND (:isActive IS NULL OR a.isActive = :isActive)
    """)
    Page<Account> searchAccounts(
            @Param("keyword") String keyword,
            @Param("roleId") Integer roleId,
            @Param("branchId") Integer branchId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
    // Lấy tất cả account theo branch ID
    List<Account> findAllByBranch_Id(Integer branchId);

    // Lấy tất cả account theo branch + role name
    List<Account> findAllByBranch_IdAndRole_RoleName(Integer branchId, String roleName);

}

