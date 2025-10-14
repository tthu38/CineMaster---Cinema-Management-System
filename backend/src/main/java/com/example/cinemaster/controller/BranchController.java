package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.BranchRequest;
import com.example.cinemaster.dto.response.BranchResponse;
import com.example.cinemaster.dto.response.BranchNameResponse;
import com.example.cinemaster.service.BranchService;
import com.example.cinemaster.repository.BranchRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final BranchRepository branchRepository;

    // CREATE
    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(@RequestBody @Valid BranchRequest request) {
        BranchResponse createdBranch = branchService.createBranch(request);
        return new ResponseEntity<>(createdBranch, HttpStatus.CREATED);
    }

    // --- READ ALL (ADMIN/MANAGER) ---
    @GetMapping
    public ResponseEntity<List<BranchResponse>> getAllBranches() {
        List<BranchResponse> branches = branchService.getAllBranches();
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BranchResponse>> getAllActiveBranches() {
        List<BranchResponse> branches = branchService.getAllActiveBranches();
        return ResponseEntity.ok(branches);
    }

    // --- READ BY ID (ADMIN) ---
    @GetMapping("/{id}/admin")
    public ResponseEntity<BranchResponse> getBranchByIdAdmin(@PathVariable Integer id) {
        BranchResponse branch = branchService.getBranchByIdForAdmin(id);
        return ResponseEntity.ok(branch);
    }

    // --- READ BY ID (CLIENT/STAFF) ---
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchByIdClient(@PathVariable Integer id) {
        BranchResponse branch = branchService.getBranchByIdForClient(id);
        return ResponseEntity.ok(branch);
    }

    // UPDATE
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Integer id,
            @RequestBody @Valid BranchRequest request) {

        BranchResponse updatedBranch = branchService.updateBranch(id, request);
        return ResponseEntity.ok(updatedBranch);
    }

    // Soft Delete
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateBranch(@PathVariable Integer id) {
        branchService.deactivateBranch(id);
        return ResponseEntity.noContent().build();
    }

    // Restore
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> activateBranch(@PathVariable Integer id) {
        branchService.activateBranch(id);
        return ResponseEntity.noContent().build();
    }

    // --- READ ALL BRANCH NAMES (for dropdown/select) ---
    @GetMapping("/names")
    public ResponseEntity<List<BranchNameResponse>> getBranchNames() {
        List<BranchNameResponse> names = branchRepository.findAllBranchNames();
        return ResponseEntity.ok(names);
    }
}
