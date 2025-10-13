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

    List<Auditorium> findByIsActiveTrue();

    List<Auditorium> findByBranch_IdAndIsActiveTrue(Integer branchId);

    Optional<Auditorium> findByAuditoriumIDAndIsActiveTrue(Integer id);

    List<Auditorium> findByBranch_Id(Integer branchId);

    @Query("""
        SELECT a FROM Auditorium a
        WHERE a.branch.id = :branchId
          AND (a.isActive = true OR a.isActive IS NULL)
    """)
    List<Auditorium> findActiveByBranch(@Param("branchId") Integer branchId);

    @Modifying
    @Query("UPDATE Auditorium a SET a.isActive = :isActive WHERE a.branch.id = :branchId")
    int updateIsActiveStatusByBranchId(
            @Param("branchId") Integer branchId,
            @Param("isActive") boolean isActive
    );
}
