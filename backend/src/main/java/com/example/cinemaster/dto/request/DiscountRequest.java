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

    @DecimalMin(value = "0.0", inclusive = false, message = "PercentOff must be greater than 0")
    @DecimalMax(value = "100.0", message = "PercentOff must be ≤ 100")
    BigDecimal percentOff;

    @DecimalMin(value = "0.0", inclusive = false, message = "FixedAmount must be greater than 0")
    BigDecimal fixedAmount;

    Integer pointCost;

    @Future(message = "Expiry date must be in the future")
    LocalDate expiryDate;

    Integer maxUsage;

    String discountStatus;

    // 🔹 Điều kiện mới: hóa đơn tối thiểu để áp mã
    @DecimalMin(value = "0.0", inclusive = true, message = "MinOrderAmount must be ≥ 0")
    BigDecimal minOrderAmount;

    // 🔹 Hạng thành viên tối thiểu được áp dụng mã
    Integer requiredLevelId;

    // 🔸 Validation logic: chỉ cho phép 1 trong 2 giá trị giảm (phần trăm hoặc cố định)
    @AssertTrue(message = "Either percentOff or fixedAmount must be provided, not both or none.")
    public boolean isValidDiscountValue() {
        return (percentOff != null ^ fixedAmount != null);
    }
}
