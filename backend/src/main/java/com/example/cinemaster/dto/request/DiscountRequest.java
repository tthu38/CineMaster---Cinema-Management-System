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

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "100.0")
    BigDecimal percentOff;

    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal fixedAmount;

    Integer pointCost;

    LocalDate createAt;     // ⭐ THÊM VÀO ĐÂY
    @Future
    LocalDate expiryDate;

    Integer maxUsage;

    String discountStatus;

    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal minOrderAmount;

    Integer requiredLevelId;   // ⭐ FE đang gửi đúng key này

    @AssertTrue(message = "Phải cung cấp percentOff hoặc fixedAmount, không được để trống hoặc nhập cả hai")
    public boolean isValidDiscountValue() {
        return (percentOff != null ^ fixedAmount != null);
    }
}
