package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketCreateRequest {

    Integer ticketId;        // Nếu null → tạo mới, nếu có → cập nhật vé tạm
    Integer accountId;       // Người đặt vé
    Integer showtimeId;      // Suất chiếu
    List<Integer> seatIds;   // Danh sách ID ghế được chọn
    List<Integer> discountIds; // Danh sách mã giảm giá áp dụng
    List<ComboItem> combos;  // Danh sách combo kèm số lượng
    private String customerEmail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComboItem {
        Integer comboId;
        Integer quantity;
    }
}
