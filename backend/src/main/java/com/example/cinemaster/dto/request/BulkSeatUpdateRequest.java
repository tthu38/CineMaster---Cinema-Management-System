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

//    @NotNull(message = "ID loại ghế mới không được để trống.")
    Integer newTypeID; // Loại ghế mới (ví dụ: VIP)

    @NotNull(message = "Ký tự dãy ghế không được để trống.")
    @Pattern(regexp = "[A-Z]", message = "Ký tự dãy phải là một chữ cái in hoa.")
    String seatRowToUpdate; // Dãy ghế muốn sửa (ví dụ: "D")
    @Pattern(regexp = "Available|Broken|Reserved", message = "Trạng thái không hợp lệ.")
    String newStatus; // Trạng thái mới (ví dụ: Broken)
    // Bạn có thể thêm trường status nếu muốn thay đổi cả trạng thái (ví dụ: Broken)
    // String newStatus;
    Boolean isConvertCoupleSeat;
    // THÊM TRƯỜNG NÀY CHO HÀNH ĐỘNG TÁCH GHẾ
    Boolean isSeparateCoupleSeat; // Cờ TÁCH ghế (đôi -> đơn)
}
