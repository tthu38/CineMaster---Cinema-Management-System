package com.example.cinemaster.dto.response;

import lombok.*;

import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkScheduleCellAssignmentResponse {
    private Integer scheduleId;
    private Integer branchId;
    private Integer accountId;
    private String  accountName;
    private LocalTime startTime;
    private LocalTime endTime;
}
