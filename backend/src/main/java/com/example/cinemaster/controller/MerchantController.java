package com.example.cinemaster.controller;

import com.example.cinemaster.entity.MerchantAccount;
import com.example.cinemaster.repository.MerchantAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchant") // ✅ đổi theo REST chuẩn
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MerchantController {
    private final MerchantAccountRepository merchantRepo;

    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAccount() {
        MerchantAccount acc = merchantRepo.findFirstByIsDefault(1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản mặc định"));

        Map<String, Object> res = Map.of(
                "bankCode", acc.getBankCode(),
                "accountNumber", acc.getAccountNumber(),
                "accountName", acc.getAccountName()
        );
        return ResponseEntity.ok(res);
    }
}