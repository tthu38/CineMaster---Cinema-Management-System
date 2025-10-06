package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkSeatRequest {

    @NotNull(message = "ID phòng chiếu không được để trống.")
    Integer auditoriumID;

    @NotNull(message = "ID loại ghế mặc định không được để trống.")
    Integer typeID; // Loại ghế mặc định cho tất cả các ghế

    @Min(value = 1, message = "Số dãy phải lớn hơn 0.")
    Integer rowCount; // Số lượng hàng (VD: 10)

    @Min(value = 1, message = "Số cột phải lớn hơn 0.")
    Integer columnCount; // Số lượng ghế trên mỗi hàng (VD: 18)

    @Pattern(regexp = "[A-Z]", message = "Ký tự dãy phải là một chữ cái in hoa.")
    String startRowChar; // Ký tự bắt đầu (VD: "A")
}
