package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountResponse {
    Integer discountID;
    String code;
    String discountDescription;
    BigDecimal percentOff;
    BigDecimal fixedAmount;
    Integer pointCost;
    LocalDate createAt;
    LocalDate expiryDate;
    Integer maxUsage;
    String discountStatus;
}
