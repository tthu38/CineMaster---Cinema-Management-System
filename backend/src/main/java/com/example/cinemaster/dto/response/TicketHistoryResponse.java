package com.example.cinemaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketHistoryResponse {
    private Integer ticketHistoryID;
    private String oldStatus;
    private String newStatus;
    private String note;
    private Instant changedAt;
    private Integer changedById;
    private String changedByName;
}
