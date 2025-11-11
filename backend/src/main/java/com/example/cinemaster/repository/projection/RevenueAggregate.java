package com.example.cinemaster.repository.projection;

import java.math.BigDecimal;

public interface RevenueAggregate {
    Long getTicketsSold();
    Long getCombosSold();
    BigDecimal getGrossBeforeDiscount();
    BigDecimal getDiscountTotal();
    BigDecimal getRevenueOnline();
    BigDecimal getRevenueCash();
    BigDecimal getTotalRevenue();
}
