package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AccountRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.dto.response.PagedResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.AccountManageService;
import com.example.cinemaster.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountManageController {

    private final AccountManageService accountService;
    private final JwtService jwtService;

    /* ========================= CREATE ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<AccountResponse> create(
            @RequestPart("data") AccountRequest request,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager")) {
            if (!request.getBranchId().equals(user.getBranchId())) {
                throw new SecurityException("Bạn chỉ có thể tạo tài khoản cho chi nhánh của mình!");
            }
            request.setBranchId(user.getBranchId());
        }

        return ResponseEntity.ok(accountService.create(request, avatarFile));
    }

    /* ========================= UPDATE ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<AccountResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") AccountRequest request,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();
        AccountResponse existing = accountService.getById(id);

        if (user.hasRole("Manager") && !existing.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể chỉnh sửa nhân viên của chi nhánh khác!");
        }

        if (user.hasRole("Manager")) {
            request.setBranchId(user.getBranchId());
        }

        return ResponseEntity.ok(accountService.update(id, request, avatarFile));
    }

    /* ========================= GET ALL (FIXED) ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping
    public ResponseEntity<PagedResponse<AccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,   // ⭐ ADDED
            Authentication auth
    ) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager")) {
            branchId = user.getBranchId();
        }

        return ResponseEntity.ok(accountService.getAllPaged(page, size, roleId, branchId, keyword, isActive));
    }

    /* ========================= GET BY ID ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable Integer id, Authentication auth) {
        AccountResponse account = accountService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !account.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể xem tài khoản của chi nhánh khác!");
        }

        return ResponseEntity.ok(account);
    }

    /* ========================= DELETE ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Integer id, Authentication auth) {
        AccountResponse account = accountService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !account.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể xóa tài khoản của chi nhánh khác!");
        }

        accountService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /* ========================= RESTORE ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Integer id, Authentication auth) {
        AccountResponse account = accountService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !account.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể khôi phục tài khoản của chi nhánh khác!");
        }

        accountService.restore(id);
        return ResponseEntity.ok().build();
    }

    /* ========================= BAN ========================= */
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/ban/{id}")
    public ResponseEntity<?> ban(@PathVariable Integer id, Authentication auth) {

        AccountResponse account = accountService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !account.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể ban tài khoản của chi nhánh khác!");
        }

        accountService.softDelete(id);
        return ResponseEntity.ok("User banned successfully!");
    }

    @GetMapping("/spam/suspicious")
    public ResponseEntity<?> getSuspiciousUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing token");
        }

        String token = authHeader.substring(7);
        String role = jwtService.extractRole(token);

        if (!role.equals("Admin") && !role.equals("Manager")) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        return ResponseEntity.ok(accountService.getSuspiciousUsers());
    }

    @GetMapping("/spam/list")
    public ResponseEntity<?> getSpamList(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing token");
        }

        String token = authHeader.substring(7);
        String role = jwtService.extractRole(token);

        if (!role.equals("Admin") && !role.equals("Manager")) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        return ResponseEntity.ok(accountService.getSpamAccountIds());
    }
}

