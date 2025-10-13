package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> create(@Valid @RequestBody DiscountRequest request) {
        DiscountResponse response = discountService.create(request);
        ApiResponse<DiscountResponse> api = ApiResponse.<DiscountResponse>builder()
                .message("Discount created successfully")
                .result(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(api);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getAll() {
        List<DiscountResponse> discounts = discountService.getAll();
        ApiResponse<List<DiscountResponse>> api = ApiResponse.<List<DiscountResponse>>builder()
                .message("Fetched all active discounts")
                .result(discounts)
                .build();
        return ResponseEntity.ok(api);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getByStatus(@PathVariable String status) {
        List<DiscountResponse> discounts = discountService.getByStatus(status);
        ApiResponse<List<DiscountResponse>> api = ApiResponse.<List<DiscountResponse>>builder()
                .message("Fetched discounts with status: " + status)
                .result(discounts)
                .build();
        return ResponseEntity.ok(api);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> getById(@PathVariable Integer id) {
        DiscountResponse discount = discountService.getById(id);
        ApiResponse<DiscountResponse> api = ApiResponse.<DiscountResponse>builder()
                .message("Fetched discount successfully")
                .result(discount)
                .build();
        return ResponseEntity.ok(api);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody DiscountRequest request) {

        DiscountResponse response = discountService.update(id, request);
        ApiResponse<DiscountResponse> api = ApiResponse.<DiscountResponse>builder()
                .message("Discount updated successfully")
                .result(response)
                .build();
        return ResponseEntity.ok(api);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/delete")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Integer id) {
        discountService.softDelete(id);
        ApiResponse<Void> api = ApiResponse.<Void>builder()
                .message("Discount soft-deleted successfully")
                .build();
        return ResponseEntity.ok(api);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Integer id) {
        discountService.restore(id);
        ApiResponse<Void> api = ApiResponse.<Void>builder()
                .message("Discount restored successfully")
                .build();
        return ResponseEntity.ok(api);
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable Integer id) {
        discountService.hardDelete(id);
        ApiResponse<Void> api = ApiResponse.<Void>builder()
                .message("Discount permanently deleted")
                .build();
        return ResponseEntity.ok(api);
    }
}
