package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ContactRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Integer> {
    List<ContactRequest> findByBranch_Id(Integer branchId);

    int countByEmailAndIsSpam(String email, Boolean isSpam);

    @Modifying
    @Transactional
    void deleteByEmailAndIsSpamTrue(String email);

}

