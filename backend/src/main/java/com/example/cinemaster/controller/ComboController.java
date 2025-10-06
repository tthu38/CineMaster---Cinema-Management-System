package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.response.ComboResponse;
import com.example.cinemaster.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    // CREATE
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> create(
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(comboService.create(request, imageFile));
    }

    // READ ALL (active trước, inactive sau)
    @GetMapping
    public ResponseEntity<List<ComboResponse>> getAll() {
        return ResponseEntity.ok(comboService.getAll());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ComboResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(comboService.getById(id));
    }

    // UPDATE
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ComboResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") ComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(comboService.update(id, request, imageFile));
    }

    // SOFT DELETE (set Available = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        comboService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // RESTORE (set Available = true)
    @PutMapping("/{id}/restore")
    public ResponseEntity<ComboResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(comboService.restore(id));
    }
}
