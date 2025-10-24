package com.example.cinemaster.repository;


import com.example.cinemaster.entity.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;


public interface ShiftSessionRepository extends JpaRepository<ShiftSession, Integer> {


    /* ============================================================
       🔹 1️⃣ TÌM CA ĐANG MỞ CỦA NHÂN VIÊN HIỆN TẠI
    ============================================================ */
    @Query("""
       SELECT s FROM ShiftSession s
       WHERE s.staff.accountID = :staffId
         AND s.status = 'OPEN'
       ORDER BY s.startTime DESC
   """)
    List<ShiftSession> findActiveSessions(@Param("staffId") Integer staffId);


    default Optional<ShiftSession> findActiveSession(Integer staffId) {
        var list = findActiveSessions(staffId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }


    /* ============================================================
       🔹 2️⃣ TÌM BẤT KỲ CA ĐANG MỞ NÀO TRONG CÙNG CHI NHÁNH
          (để nhân viên khác cùng branch có thể xem chung)
    ============================================================ */
    @Query("""
       SELECT s FROM ShiftSession s
       WHERE s.staff.branch.id = :branchId
         AND s.status = 'OPEN'
       ORDER BY s.startTime DESC
   """)
    Optional<ShiftSession> findAnyOpenSessionInSameBranch(@Param("branchId") Integer branchId);
}

