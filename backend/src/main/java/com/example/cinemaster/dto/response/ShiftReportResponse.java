package com.example.cinemaster.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftReportResponse {
    BigDecimal openingCash;
    int soldSeats;
    BigDecimal ticketRevenue;
    int soldCombos;
    BigDecimal comboRevenue;
    BigDecimal discountTotal;
    BigDecimal revenueCash;
    BigDecimal revenueTransfer;
}
