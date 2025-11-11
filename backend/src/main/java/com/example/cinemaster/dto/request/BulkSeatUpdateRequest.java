package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkSeatUpdateRequest {

    @NotNull(message = "ID phòng chiếu không được để trống.")
    Integer auditoriumID;
    Integer newTypeID;
    @NotNull(message = "Ký tự dãy ghế không được để trống.")
    @Pattern(regexp = "[A-Z]", message = "Ký tự dãy phải là một chữ cái in hoa.")
    String seatRowToUpdate;
    @Pattern(regexp = "Available|Broken|Reserved", message = "Trạng thái không hợp lệ.")
    String newStatus;
    Boolean isConvertCoupleSeat;
    Boolean isSeparateCoupleSeat;
}
