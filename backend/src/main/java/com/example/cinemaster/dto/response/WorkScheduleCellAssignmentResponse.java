package com.example.cinemaster.dto.response;

import lombok.*;

import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkScheduleCellAssignmentResponse {
    private Integer scheduleId;  // có thể null nếu chỉ dựng từ join
    private Integer branchId;
    private Integer accountId;
    private String  accountName; // để FE hiển thị tên
    private LocalTime startTime;   // thêm
    private LocalTime endTime;
}
