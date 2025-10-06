package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {
    // JpaRepository provides: save, findById, findAll, deleteById, etc.
    // 1. Tìm tất cả các Branch CÒN HOẠT ĐỘNG (IsActive = true)
    List<Branch> findByIsActiveTrue();

    // 2. Tìm Branch theo ID, CHỈ nếu nó CÒN HOẠT ĐỘNG
    Optional<Branch> findByIdAndIsActiveTrue(Integer id);
}
