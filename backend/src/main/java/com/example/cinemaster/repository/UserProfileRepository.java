package com.example.cinemaster.repository;

import com.example.cinemaster.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserProfileRepository extends JpaRepository<Account, Integer> {

    // Update email, password, fullName, phoneNumber, address, avatarUrl
    @Modifying
    @Transactional
    @Query("UPDATE Account a SET " +
            "a.email = :email, " +
            "a.password = :password, " +
            "a.fullName = :fullName, " +
            "a.phoneNumber = :phoneNumber, " +
            "a.address = :address, " +
            "a.avatarUrl = :avatarUrl " +
            "WHERE a.accountID = :accountID")
    int updateProfile(Integer accountID,
                      String email,
                      String password,
                      String fullName,
                      String phoneNumber,
                      String address,
                      String avatarUrl);
}
