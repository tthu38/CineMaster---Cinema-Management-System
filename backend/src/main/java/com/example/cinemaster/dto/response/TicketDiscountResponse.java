package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketDiscountResponse {

    Integer ticketId;          // ID của vé
    String discountCode;       // Mã giảm giá được áp dụng
    BigDecimal discountAmount; // Số tiền được giảm
    BigDecimal newTotal;       // Tổng mới sau khi giảm
    BigDecimal originalTotal;  // Tổng gốc (ghế + combo, chưa giảm)

    // 🆕 Chi tiết tách riêng (hữu ích cho FE hiển thị)
    BigDecimal seatPrice;      // Tổng tiền ghế
    BigDecimal comboPrice;     // Tổng tiền combo
}
