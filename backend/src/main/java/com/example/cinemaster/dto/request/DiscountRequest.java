package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRequest {

    @NotBlank(message = "Code is required")
    String code;

    String discountDescription;

    @DecimalMin(value = "0.0", inclusive = false, message = "PercentOff phải lớn hơn 0")
    @DecimalMax(value = "100.0", message = "PercentOff phải bé hơn 100")
    BigDecimal percentOff;

    @DecimalMin(value = "0.0", inclusive = false, message = "FixedAmount phải lớn hơn 0")
    BigDecimal fixedAmount;

    Integer pointCost;

    @Future(message = "Ngày hết hạn phải ở trong tương lai")
    LocalDate expiryDate;

    Integer maxUsage;

    String discountStatus;

    @DecimalMin(value = "0.0", inclusive = true, message = "MinOrderAmount phải ≥ 0")
    BigDecimal minOrderAmount;
    Integer requiredLevelId;

    @AssertTrue(message = "Phải cung cấp percentOff hoặc fixedAmount, không phải cả hai hoặc không có.")
    public boolean isValidDiscountValue() {
        return (percentOff != null ^ fixedAmount != null);
    }
}
