package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.BranchRequest;
import com.example.cinemaster.dto.response.BranchResponse;
import com.example.cinemaster.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    // --- CREATE ---
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(@RequestBody @Valid BranchRequest request) {
        BranchResponse createdBranch = branchService.createBranch(request);
        return new ResponseEntity<>(createdBranch, HttpStatus.CREATED);
    }

    // --- READ ALL (ADMIN/MANAGER) ---
    /**
     * GET /api/v1/branches : Lấy TẤT CẢ chi nhánh (Active & Inactive). Yêu cầu quyền ADMIN/MANAGER.
     */
    @GetMapping
    public ResponseEntity<List<BranchResponse>> getAllBranches() {
        List<BranchResponse> branches = branchService.getAllBranches();
        return ResponseEntity.ok(branches);
    }

    // --- READ ALL ACTIVE (CLIENT/STAFF) ---
    /**
     * GET /api/v1/branches/active : Lấy tất cả chi nhánh ĐANG HOẠT ĐỘNG. Yêu cầu quyền CLIENT/STAFF.
     */
    @GetMapping("/active")
    public ResponseEntity<List<BranchResponse>> getAllActiveBranches() {
        List<BranchResponse> branches = branchService.getAllActiveBranches();
        return ResponseEntity.ok(branches);
    }

    // --- READ BY ID (Đã tách thành 2 endpoint) ---

    /**
     * GET /api/v1/branches/{id}/admin : Lấy chi nhánh theo ID (Bao gồm cả đã đóng). Yêu cầu quyền ADMIN.
     */
    @GetMapping("/{id}/admin")
    public ResponseEntity<BranchResponse> getBranchByIdAdmin(@PathVariable Integer id) {
        BranchResponse branch = branchService.getBranchByIdForAdmin(id);
        return ResponseEntity.ok(branch);
    }

    /**
     * GET /api/v1/branches/{id} : Lấy chi nhánh theo ID (CHỈ NẾU ĐANG HOẠT ĐỘNG). Yêu cầu quyền CLIENT/STAFF.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchByIdClient(@PathVariable Integer id) {
        BranchResponse branch = branchService.getBranchByIdForClient(id);
        return ResponseEntity.ok(branch);
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Integer id,
            @RequestBody @Valid BranchRequest request) {

        BranchResponse updatedBranch = branchService.updateBranch(id, request);
        return ResponseEntity.ok(updatedBranch);
    }

    // --- DEACTIVATE (Xóa Mềm) ---
    /**
     * DELETE /api/v1/branches/{id} : Vô hiệu hóa (Xóa Mềm) chi nhánh.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateBranch(@PathVariable Integer id) {
        branchService.deactivateBranch(id);
        return ResponseEntity.noContent().build();
    }

    // --- ACTIVATE (Kích hoạt lại) ---
    /**
     * POST /api/v1/branches/{id}/activate : Kích hoạt lại chi nhánh đã đóng.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateBranch(@PathVariable Integer id) {
        branchService.activateBranch(id);
        return ResponseEntity.noContent().build();
    }
}