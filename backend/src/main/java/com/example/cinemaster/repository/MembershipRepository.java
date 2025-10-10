package com.example.cinemaster.repository;


import com.example.cinemaster.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;


@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    Optional<Membership> findByAccount_AccountID(Integer accountId);
}