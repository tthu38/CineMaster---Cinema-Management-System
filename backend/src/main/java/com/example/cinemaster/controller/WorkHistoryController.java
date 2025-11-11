package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.WorkHistoryCreateRequest;
import com.example.cinemaster.dto.request.WorkHistoryUpdateRequest;
import com.example.cinemaster.dto.response.PageResponse;
import com.example.cinemaster.dto.response.WorkHistoryResponse;
import com.example.cinemaster.service.WorkHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/work-histories")
public class WorkHistoryController {

    private final WorkHistoryService service;

    @PostMapping
    public ResponseEntity<WorkHistoryResponse> create(@Valid @RequestBody WorkHistoryCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkHistoryResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkHistoryResponse>> search(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) Integer affectedAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "actionTime,ASC") String sort
    ) {
        String[] s = sort.split(",");
        Sort sortObj = (s.length == 2) ? Sort.by(Sort.Direction.fromString(s[1]), s[0])
                : Sort.by("actionTime").ascending();
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(service.search(accountId, affectedAccountId, from, to, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkHistoryResponse> update(@PathVariable Integer id,
                                                      @Valid @RequestBody WorkHistoryUpdateRequest req) {
        req = new WorkHistoryUpdateRequest(id, req.affectedAccountId(), req.action(), req.actionTime(), req.description());
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
