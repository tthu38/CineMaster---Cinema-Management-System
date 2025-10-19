package com.example.cinemaster.repository;

import com.example.cinemaster.entity.MerchantAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantAccountRepository extends JpaRepository<MerchantAccount, Integer> {

    Optional<MerchantAccount> findFirstByIsDefault(Integer isDefault);
}
