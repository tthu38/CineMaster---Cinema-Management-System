package com.example.cinemaster.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueRowResponse {

    private String label;
    private LocalDateTime from;
    private LocalDateTime to;

    private Long ticketsSold;
    private long combosSold;

    @Builder.Default
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal comboRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal grossBeforeDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal discountTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal revenueOnline = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal revenueCash = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;
}
