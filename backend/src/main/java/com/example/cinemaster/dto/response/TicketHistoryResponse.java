package com.example.cinemaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketHistoryResponse {
    private Integer ticketHistoryID;
    private String oldStatus;
    private String newStatus;
    private String note;
    private LocalDateTime changedAt; // ✅ đổi Instant → LocalDateTime
    private Integer changedById;
    private String changedByName;
}
