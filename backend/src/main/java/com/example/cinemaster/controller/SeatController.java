package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.BulkSeatRequest;
import com.example.cinemaster.dto.request.BulkSeatUpdateRequest;
import com.example.cinemaster.dto.request.SeatRequest;
import com.example.cinemaster.dto.response.SeatResponse;
import com.example.cinemaster.service.SeatService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // GET: /api/v1/seats
    @GetMapping
    public List<SeatResponse> getAllSeats() {
        return seatService.getAllSeats();
    }

    // GET: /api/v1/seats/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Integer id) {
        try {
            SeatResponse response = seatService.getSeatById(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        }
    }

    // POST: /api/v1/seats
    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody SeatRequest request) {
        try {
            SeatResponse created = seatService.createSeat(request);
            return new ResponseEntity<>(created, HttpStatus.CREATED); // 201
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // 400 Bad Request
        }
    }

    // PUT: /api/v1/seats/{id}
    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(@PathVariable Integer id, @Valid @RequestBody SeatRequest request) {
        try {
            SeatResponse updated = seatService.updateSeat(id, request);
            return ResponseEntity.ok(updated); // 200
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        }
    }

    // DELETE: /api/v1/seats/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Integer id) {
        try {
            seatService.deleteSeat(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        }
    }

    // ENDPOINT MỚI CHO TẠO HÀNG LOẠT
    @PostMapping("/bulk") // API: POST /api/v1/seats/bulk
    public ResponseEntity<List<SeatResponse>> createBulkSeats(@Valid @RequestBody BulkSeatRequest request) {
        List<SeatResponse> seats = seatService.createBulkSeats(request);
        return new ResponseEntity<>(seats, HttpStatus.CREATED);
    }

    @PutMapping("/bulk-update-row") // API: PUT /api/v1/seats/bulk-update-row
    public ResponseEntity<List<SeatResponse>> bulkUpdateSeatRow(@Valid @RequestBody BulkSeatUpdateRequest request) {
        List<SeatResponse> seats = seatService.bulkUpdateSeatRow(request);
        return new ResponseEntity<>(seats, HttpStatus.OK);
    }
}