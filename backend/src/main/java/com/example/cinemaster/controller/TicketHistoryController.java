package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.TicketHistoryResponse;
import com.example.cinemaster.repository.TicketHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ticket-history")
@RequiredArgsConstructor
public class TicketHistoryController {

    private final TicketHistoryRepository ticketHistoryRepository;

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<List<TicketHistoryResponse>>> getHistory(@PathVariable Integer ticketId) {
        List<TicketHistoryResponse> list = ticketHistoryRepository.findByTicketIdOrdered(ticketId)
                .stream()
                .map(h -> TicketHistoryResponse.builder()
                        .ticketHistoryID(h.getTicketHistoryID())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt()) // ✅ CHỈ CẦN DÒNG NÀY
                        .changedById(h.getChangedBy() != null ? h.getChangedBy().getAccountID() : null)
                        .changedByName(h.getChangedBy() != null
                                ? h.getChangedBy().getFullName()
                                : "Khách hàng")
                        .build())

                .toList();

        return ResponseEntity.ok(ApiResponse.<List<TicketHistoryResponse>>builder()
                .code(1000)
                .message("Success")
                .result(list)
                .build());
    }
}
