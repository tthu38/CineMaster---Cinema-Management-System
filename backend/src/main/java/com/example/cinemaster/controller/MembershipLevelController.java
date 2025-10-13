package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.MembershipLevelRequest;
import com.example.cinemaster.dto.response.MembershipLevelResponse;
import com.example.cinemaster.dto.response.PageResponse;
import com.example.cinemaster.service.MembershipLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/membership-levels")
@RequiredArgsConstructor
@Validated
public class MembershipLevelController {

    private final MembershipLevelService service;

    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<MembershipLevelResponse> create(@Valid @RequestBody MembershipLevelRequest request) {
        MembershipLevelResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembershipLevelResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MembershipLevelResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<MembershipLevelResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody MembershipLevelRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
