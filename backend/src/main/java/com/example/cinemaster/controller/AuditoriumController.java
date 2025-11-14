package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AuditoriumRequest;
import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.service.AuditoriumService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoriums")
@RequiredArgsConstructor
public class AuditoriumController {

    private final AuditoriumService auditoriumService;

    @GetMapping("/active")
    public ResponseEntity<List<AuditoriumResponse>> getAllActiveAuditoriums() {
        return ResponseEntity.ok(auditoriumService.getAllActiveAuditoriums());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdClient(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(auditoriumService.getAuditoriumByIdForClient(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/branch/{branchId}/active")
    public ResponseEntity<List<AuditoriumResponse>> getActiveAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.ok(auditoriumService.getActiveAuditoriumsByBranchId(branchId));
    }


    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping
    public ResponseEntity<List<AuditoriumResponse>> getAllAuditoriums() {
        return ResponseEntity.ok(auditoriumService.getAllAuditoriums());
    }

    // Lọc phòng chiếu theo chi nhánh (có thể có branchId = null)
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/branch")
    public ResponseEntity<List<AuditoriumResponse>> listByBranch(
            @RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(auditoriumService.listByBranch(branchId));
    }

    // Lấy tất cả phòng chiếu theo chi nhánh cụ thể
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<AuditoriumResponse>> getAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.ok(auditoriumService.getAuditoriumsByBranchId(branchId));
    }

    // Lấy chi tiết phòng chiếu (Admin xem được kể cả phòng bị khóa)
    @PreAuthorize("hasAnyRole('Admin','Manager','Staff')")
    @GetMapping("/{id}/admin")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdAdmin(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(auditoriumService.getAuditoriumByIdForAdmin(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ADMIN — CRUD quản lý phòng chiếu
    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<AuditoriumResponse> createAuditorium(
            @Valid @RequestBody AuditoriumRequest request) {
        try {
            AuditoriumResponse created = auditoriumService.createAuditorium(request);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> updateAuditorium(
            @PathVariable Integer id,
            @Valid @RequestBody AuditoriumRequest request) {
        try {
            return ResponseEntity.ok(auditoriumService.updateAuditorium(id, request));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAuditorium(@PathVariable Integer id) {
        try {
            auditoriumService.deactivateAuditorium(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateAuditorium(@PathVariable Integer id) {
        try {
            auditoriumService.activateAuditorium(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
