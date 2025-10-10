package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AuditoriumRequest;
import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.service.AuditoriumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoriums")
@RequiredArgsConstructor
public class AuditoriumController {

    private final AuditoriumService auditoriumService;

    @GetMapping
    public ResponseEntity<List<AuditoriumResponse>> list(@RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(auditoriumService.listByBranch(branchId));
    }

    // --- READ ALL (CHO ADMIN/MANAGER) ---
    @GetMapping
    public List<AuditoriumResponse> getAllAuditoriums() {
        return auditoriumService.getAllAuditoriums();
    }

    @GetMapping("/active")
    public List<AuditoriumResponse> getAllActiveAuditoriums() {
        return auditoriumService.getAllActiveAuditoriums();
    }

    @GetMapping("/{id}/admin")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdAdmin(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(auditoriumService.getAuditoriumByIdForAdmin(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdClient(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(auditoriumService.getAuditoriumByIdForClient(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/branch/{branchId}")
    public List<AuditoriumResponse> getAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return auditoriumService.getAuditoriumsByBranchId(branchId);
    }

    @GetMapping("/branch/{branchId}/active")
    public List<AuditoriumResponse> getActiveAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return auditoriumService.getActiveAuditoriumsByBranchId(branchId);
    }


    @PostMapping
    public ResponseEntity<AuditoriumResponse> createAuditorium(@Valid @RequestBody AuditoriumRequest request) {
        try {
            return new ResponseEntity<>(auditoriumService.createAuditorium(request), HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> updateAuditorium(@PathVariable Integer id,
                                                               @Valid @RequestBody AuditoriumRequest request) {
        try {
            return ResponseEntity.ok(auditoriumService.updateAuditorium(id, request));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

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
