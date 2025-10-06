package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditoriumRequest {

    @NotBlank(message = "Tên phòng chiếu không được để trống.")
    String name;

    @NotNull(message = "Sức chứa không được để trống.")
    @Min(value = 10, message = "Sức chứa tối thiểu là 10 ghế.")
    Integer capacity;

    @NotBlank(message = "Loại phòng không được để trống.")
    String type; // Ví dụ: 2D, 3D, IMAX, VIP

    @NotNull(message = "ID Chi nhánh không được để trống.")
    Integer branchID; // Khóa ngoại, ID của Branch (kiểu Integer theo Entity của bạn)
}
