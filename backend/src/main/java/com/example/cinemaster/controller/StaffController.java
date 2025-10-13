package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.StaffSimpleResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staffs")
public class StaffController {
    private final AccountRepository accountRepo;

    // StaffController.java
    @GetMapping
    public ResponseEntity<List<StaffSimpleResponse>> listByBranch(@RequestParam Integer branchId) {
        if (branchId == null) {
            return ResponseEntity.badRequest().build(); // hoặc throw
        }
        List<Account> staff = accountRepo.findStaffsByBranch(branchId);
        return ResponseEntity.ok(
                staff.stream()
                        .map(a -> new StaffSimpleResponse(a.getAccountID(), a.getFullName()))
                        .toList()
        );
    }

}
