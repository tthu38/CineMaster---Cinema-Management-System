package com.example.cinemaster.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftSessionResponse {
    Integer id;
    Integer staffId;
    String staffName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal openingCash;
    BigDecimal closingCash;
    BigDecimal expectedCash;
    BigDecimal difference;
    String status;
}
