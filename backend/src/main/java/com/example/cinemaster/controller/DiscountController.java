package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest request) {
        return new ResponseEntity<>(discountService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> getAll() {
        return ResponseEntity.ok(discountService.getAll());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DiscountResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(discountService.getByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(discountService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.update(id, request));
    }

    // SOFT DELETE
    @PutMapping("/{id}/delete")
    public ResponseEntity<Void> softDelete(@PathVariable Integer id) {
        discountService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // RESTORE
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Integer id) {
        discountService.restore(id);
        return ResponseEntity.noContent().build();
    }

    // HARD DELETE (optional)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable Integer id) {
        discountService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
