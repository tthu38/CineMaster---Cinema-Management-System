package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.response.ComboResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;


    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> create(
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication auth) {

        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager")) {
            // 🔒 Gán luôn branchId của Manager để tránh fake dữ liệu
            request.setBranchId(user.getBranchId());
        }

        return ResponseEntity.ok(comboService.create(request, imageFile));
    }

    @GetMapping
    public ResponseEntity<List<ComboResponse>> getAll() {
        return ResponseEntity.ok(comboService.getAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<ComboResponse>> getAvailable() {
        return ResponseEntity.ok(comboService.getAvailable());
    }


    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ComboResponse>> getByBranch(@PathVariable Integer branchId, Authentication auth) {
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // ✅ Manager chỉ xem combo của chi nhánh mình
        if (user.hasRole("Manager") && !branchId.equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể xem combo của chi nhánh khác!");
        }

        return ResponseEntity.ok(comboService.getByBranch(branchId));
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @GetMapping("/{id}")
    public ResponseEntity<ComboResponse> getById(@PathVariable Integer id, Authentication auth) {
        ComboResponse combo = comboService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !combo.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể xem combo của chi nhánh khác!");
        }

        return ResponseEntity.ok(combo);
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication auth) {

        ComboResponse existing = comboService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        // ✅ Manager chỉ được sửa combo của chi nhánh mình
        if (user.hasRole("Manager") && !existing.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể sửa combo của chi nhánh khác!");
        }

        return ResponseEntity.ok(comboService.update(id, request, imageFile));
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication auth) {
        ComboResponse existing = comboService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !existing.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể xóa combo của chi nhánh khác!");
        }

        comboService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('Admin','Manager')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ComboResponse> restore(@PathVariable Integer id, Authentication auth) {
        ComboResponse existing = comboService.getById(id);
        AccountPrincipal user = (AccountPrincipal) auth.getPrincipal();

        if (user.hasRole("Manager") && !existing.getBranchId().equals(user.getBranchId())) {
            throw new SecurityException("Bạn không thể khôi phục combo của chi nhánh khác!");
        }

        return ResponseEntity.ok(comboService.restore(id));
    }
}
