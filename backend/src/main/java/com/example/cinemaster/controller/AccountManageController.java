package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AccountRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.dto.response.PagedResponse;
import com.example.cinemaster.service.AccountManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountManageController {

    private final AccountManageService accountService;

    // CREATE
    @PreAuthorize("hasRole('Admin')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<AccountResponse> create(
            @RequestPart("data") AccountRequest request,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        return ResponseEntity.ok(accountService.create(request, avatarFile));
    }

    // UPDATE
    @PreAuthorize("hasRole('Admin')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<AccountResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") AccountRequest request,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        return ResponseEntity.ok(accountService.update(id, request, avatarFile));
    }

    // READ ALL (only active)
    @PreAuthorize("hasRole('Admin')")
    @GetMapping
    public ResponseEntity<PagedResponse<AccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(accountService.getAllPaged(page, size, roleId, branchId, keyword));
    }



    // READ BY ID
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    // SOFT DELETE
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Integer id) {
        accountService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // RESTORE
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Integer id) {
        accountService.restore(id);
        return ResponseEntity.ok().build();
    }
}
