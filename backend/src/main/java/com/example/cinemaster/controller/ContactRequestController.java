package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ContactRequestRequest;
import com.example.cinemaster.dto.request.ContactUpdateRequest;
import com.example.cinemaster.dto.response.ContactRequestResponse;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.ContactRequestService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestService service;

    //  Gửi yêu cầu (public)
    @PermitAll
    @PostMapping
    public ResponseEntity<ContactRequestResponse> create(@Valid @RequestBody ContactRequestRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    //  Staff xem liên hệ chi nhánh mình
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ContactRequestResponse>> getByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.ok(service.getByBranch(branchId));
    }

    //  Staff xử lý contact
    @PreAuthorize("hasRole('Staff')")
    @PutMapping("/{contactId}/update")
    public ResponseEntity<ContactRequestResponse> updateStatus(
            @PathVariable Integer contactId,
            @Valid @RequestBody ContactUpdateRequest dto,
            Authentication auth
    ) {
        AccountPrincipal staff = (AccountPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(service.updateStatus(contactId, dto, staff.getId()));
    }

    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    @GetMapping("/{id}")
    public ResponseEntity<ContactRequestResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/all")
    public ResponseEntity<List<ContactRequestResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }



}
