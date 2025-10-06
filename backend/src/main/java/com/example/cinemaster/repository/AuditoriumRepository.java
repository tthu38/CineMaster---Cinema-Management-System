package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Integer> {
    // 1. Tìm tất cả các phòng chiếu CÒN HOẠT ĐỘNG
    List<Auditorium> findByIsActiveTrue();

    // 2. Tìm phòng chiếu theo Branch ID và CÒN HOẠT ĐỘNG
    List<Auditorium> findByBranch_IdAndIsActiveTrue(Integer branchId);

    // 3. Tìm phòng chiếu theo ID, CHỈ nếu nó CÒN HOẠT ĐỘNG
    Optional<Auditorium> findByAuditoriumIDAndIsActiveTrue(Integer id);

    // Phương thức cũ (giữ lại)
    List<Auditorium> findByBranch_Id(Integer branchId);

    @Modifying
    // 🔥 Sửa thành a.branch.id (thuộc tính branch trong Auditorium -> thuộc tính id trong Branch)
    @Query("UPDATE Auditorium a SET a.isActive = :isActive WHERE a.branch.id = :branchId")
    int updateIsActiveStatusByBranchId(
            @Param("branchId") Integer branchId, // Dùng Integer để khớp với Branch.id
            @Param("isActive") boolean isActive
    );
}
