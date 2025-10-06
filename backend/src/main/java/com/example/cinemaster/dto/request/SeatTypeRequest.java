package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatTypeRequest {

    @NotBlank(message = "Tên loại ghế không được để trống.")
    @Pattern(regexp = "Normal|VIP|Couple|OtherType", message = "Tên loại ghế không hợp lệ.")
    String typeName;

    @NotNull(message = "Hệ số giá không được để trống.")
    @DecimalMin(value = "1.00", message = "Hệ số giá phải lớn hơn hoặc bằng 1.00")
    BigDecimal priceMultiplier;
}