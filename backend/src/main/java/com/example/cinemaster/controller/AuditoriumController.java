package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AuditoriumRequest;
import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.service.AuditoriumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoriums")
@CrossOrigin(origins = {"http://localhost:63342", "http://127.0.0.1:63342", "http://localhost:3000"})
public class AuditoriumController {

    private final AuditoriumService auditoriumService;

    public AuditoriumController(AuditoriumService auditoriumService) {
        this.auditoriumService = auditoriumService;
    }

    // --- READ ALL (CHO ADMIN/MANAGER) ---
    // GET: /api/v1/auditoriums
    @GetMapping
    public List<AuditoriumResponse> getAllAuditoriums() {
        return auditoriumService.getAllAuditoriums(); // Lấy tất cả (Active & Inactive)
    }

    // --- READ ALL ACTIVE (CHO CLIENT/STAFF) ---
    // GET: /api/v1/auditoriums/active
    @GetMapping("/active")
    public List<AuditoriumResponse> getAllActiveAuditoriums() {
        return auditoriumService.getAllActiveAuditoriums(); // Chỉ lấy Active
    }

    // --- READ ONE (ĐÃ TÁCH) ---
    // GET: /api/v1/auditoriums/{id}/admin (Xem cả đã đóng)
    @GetMapping("/{id}/admin")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdAdmin(@PathVariable Integer id) {
        try {
            AuditoriumResponse response = auditoriumService.getAuditoriumByIdForAdmin(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET: /api/v1/auditoriums/{id} (Chỉ xem cái đang hoạt động)
    @GetMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> getAuditoriumByIdClient(@PathVariable Integer id) {
        try {
            AuditoriumResponse response = auditoriumService.getAuditoriumByIdForClient(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- READ BY BRANCH ID (ĐÃ TÁCH) ---
    // GET: /api/v1/auditoriums/branch/{branchId} (Lấy tất cả của Branch, cho Admin)
    @GetMapping("/branch/{branchId}")
    public List<AuditoriumResponse> getAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return auditoriumService.getAuditoriumsByBranchId(branchId);
    }

    // GET: /api/v1/auditoriums/branch/{branchId}/active (Chỉ lấy đang hoạt động, cho Client)
    @GetMapping("/branch/{branchId}/active")
    public List<AuditoriumResponse> getActiveAuditoriumsByBranchId(@PathVariable Integer branchId) {
        return auditoriumService.getActiveAuditoriumsByBranchId(branchId);
    }

    // POST: /api/v1/auditoriums (Giữ nguyên)
    @PostMapping
    public ResponseEntity<AuditoriumResponse> createAuditorium(@Valid @RequestBody AuditoriumRequest request) {
        try {
            AuditoriumResponse created = auditoriumService.createAuditorium(request);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // PUT: /api/v1/auditoriums/{id} (Giữ nguyên)
    @PutMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> updateAuditorium(@PathVariable Integer id, @Valid @RequestBody AuditoriumRequest request) {
        try {
            AuditoriumResponse updated = auditoriumService.updateAuditorium(id, request);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            // Xử lý lỗi khi cố gắng update một phòng chiếu đã đóng
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // --- DEACTIVATE (XÓA MỀM - Thay thế DELETE) ---
    // DELETE: /api/v1/auditoriums/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAuditorium(@PathVariable Integer id) {
        try {
            auditoriumService.deactivateAuditorium(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        } catch (RuntimeException e) {
            // Xử lý lỗi khi cố gắng đóng một phòng chiếu đã đóng
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }
    }

    // --- ACTIVATE (Kích hoạt lại) ---
    // POST: /api/v1/auditoriums/{id}/activate
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateAuditorium(@PathVariable Integer id) {
        try {
            auditoriumService.activateAuditorium(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        } catch (RuntimeException e) {
            // Xử lý lỗi khi cố gắng mở một phòng chiếu đang mở
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }
    }
}