package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.response.ComboResponse;
import com.example.cinemaster.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    // ===== CREATE =====
    @PreAuthorize("hasRole('Admin')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> create(
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(comboService.create(request, imageFile));
    }

    // ===== READ ALL (active trước, inactive sau) =====
    @GetMapping
    public ResponseEntity<List<ComboResponse>> getAll() {
        return ResponseEntity.ok(comboService.getAll());
    }

    // ===== READ ONLY ACTIVE (frontend) =====
    @GetMapping("/available")
    public ResponseEntity<List<ComboResponse>> getAvailable() {
        return ResponseEntity.ok(comboService.getAvailable());
    }

    // ===== READ BY BRANCH (staff/admin) =====
    @PreAuthorize("hasAnyRole('Admin','Staff')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ComboResponse>> getByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.ok(comboService.getByBranch(branchId));
    }

    // ===== READ BY ID =====
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/{id}")
    public ResponseEntity<ComboResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(comboService.getById(id));
    }

    // ===== UPDATE =====
    @PreAuthorize("hasRole('Admin')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(comboService.update(id, request, imageFile));
    }

    // ===== SOFT DELETE =====
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        comboService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== RESTORE =====
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ComboResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(comboService.restore(id));
    }
}
