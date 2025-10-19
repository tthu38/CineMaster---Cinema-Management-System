package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatRequest {

    @NotNull(message = "ID phòng chiếu không được để trống.")
    Integer auditoriumID; // Khóa ngoại tới Auditorium

    @NotNull(message = "ID loại ghế không được để trống.")
    Integer typeID; // Khóa ngoại tới SeatType

    @NotBlank(message = "Số ghế không được để trống.")
    @Size(max = 10, message = "Số ghế tối đa 10 ký tự.")
    String seatNumber; // Ví dụ: A1, B10, etc.

    @NotBlank(message = "Dãy ghế không được để trống.")
    @Size(max = 10, message = "Dãy ghế tối đa 10 ký tự.")
    String seatRow; // Ví dụ: A, B, C

    @NotNull(message = "Cột ghế không được để trống.")
    Integer columnNumber;

    @NotBlank(message = "Trạng thái không được để trống.")
    @Pattern(regexp = "Available|Broken|Reserved", message = "Trạng thái phải là Available, Broken hoặc Reserved.")
    String status;
}