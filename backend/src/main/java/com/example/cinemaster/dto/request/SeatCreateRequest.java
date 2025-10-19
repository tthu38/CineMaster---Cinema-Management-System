package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatCreateRequest {

    @NotNull(message = "ID phòng chiếu không được để trống.")
    Integer auditoriumID;

    @NotNull(message = "ID loại ghế không được để trống.")
    Integer typeID;

    @NotBlank(message = "Số ghế không được để trống.")
    @Size(max = 10, message = "Số ghế tối đa 10 ký tự.")
    String seatNumber;

    @NotBlank(message = "Dãy ghế không được để trống.")
    @Size(max = 10, message = "Dãy ghế tối đa 10 ký tự.")
    String seatRow;

    @NotNull(message = "Cột ghế không được để trống.")
    Integer columnNumber;

    @NotBlank(message = "Trạng thái không được để trống.")
    @Pattern(regexp = "Available|Broken|Reserved|Booked",
            message = "Trạng thái phải là Available, Broken, Reserved hoặc Booked.")
    String status;
}
