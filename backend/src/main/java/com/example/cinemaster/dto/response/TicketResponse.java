package com.example.cinemaster.dto.response;

import com.example.cinemaster.entity.Ticket;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketResponse {
    Integer ticketId;
    Integer accountId;
    Integer showtimeId;
    Ticket.TicketStatus status;

    // 💰 Tổng tiền các loại
    BigDecimal seatTotal;        // 💺 Tổng tiền ghế
    BigDecimal comboTotal;       // 🍿 Tổng tiền combo
    BigDecimal originalPrice;    // 💸 Giá gốc (ghế + combo)
    BigDecimal discountTotal;    // 🔻 Tổng tiền giảm
    BigDecimal totalPrice;       // ✅ Tổng cuối sau giảm

    // 💳 Thông tin thanh toán / thời gian
    String paymentMethod;
    LocalDateTime bookingTime;
    LocalDateTime holdUntil;

    // 🎟️ Danh sách chi tiết vé
    List<Integer> seatIds;
    List<ComboResponse> combos;
    List<DiscountResponse> discounts;

    // 🎬 Thông tin phim & rạp
    String movieTitle;
    String auditoriumName;
    String seatNames;
    String branchAddress;
    LocalDateTime startTime;

    // ================== 🍿 Inner Classes ==================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComboResponse {
        Integer comboId;
        String comboName;
        Integer quantity;
        BigDecimal price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiscountResponse {
        Integer discountId;
        String discountName;
        BigDecimal amount;
    }
}
