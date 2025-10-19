// src/main/java/com/example/cinemaster/repository/projection/RevenueAggregate.java
package com.example.cinemaster.repository.projection;

import java.math.BigDecimal;

public interface RevenueAggregate {
    Long getTicketsSold();          // 1️⃣
    BigDecimal getTicketRevenue();  // 2️⃣
    Long getCombosSold();           // 3️⃣
    BigDecimal getComboRevenue();   // 4️⃣
    BigDecimal getDiscountTotal();  // 5️⃣
    BigDecimal getRevenueOnline();  // 6️⃣
    BigDecimal getRevenueCash();    // 7️⃣
    BigDecimal getTotalRevenue();
}

