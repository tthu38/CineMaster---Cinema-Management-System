package com.example.cinemaster.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueDayResponse {
    private LocalDate date;
    private BigDecimal totalRevenue;
}
