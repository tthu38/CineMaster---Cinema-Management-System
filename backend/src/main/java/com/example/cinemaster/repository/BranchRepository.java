package com.example.cinemaster.repository;

import com.example.cinemaster.dto.response.BranchNameResponse;
import com.example.cinemaster.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Integer> {

    @Query("SELECT new com.example.cinemaster.dto.response.BranchNameResponse(b.id, b.branchName) " +
            "FROM Branch b")
    List<BranchNameResponse> findAllBranchNames();
}
