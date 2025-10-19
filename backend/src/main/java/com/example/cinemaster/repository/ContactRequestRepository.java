package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Integer> {
    List<ContactRequest> findByBranch_Id(Integer branchId);
}
