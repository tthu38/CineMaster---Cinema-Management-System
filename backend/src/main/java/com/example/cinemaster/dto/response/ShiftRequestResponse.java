package com.example.cinemaster.dto.response;


import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftRequestResponse {
    private Integer requestID;
    private Integer accountId;
    private String accountName;
    private Integer branchId;
    private String branchName;
    private LocalDate shiftDate;
    private String shiftType;
    private String status;
    private String note;
    private LocalDateTime createdAt;
}



