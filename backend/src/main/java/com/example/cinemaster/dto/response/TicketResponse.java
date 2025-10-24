package com.example.cinemaster.dto.response;

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

    // 🏢 Thông tin mở rộng cho staff & giao diện tổng quan
    String branchName;       // ✅ Tên chi nhánh
    String showtimeStart;    // ✅ Thời gian chiếu dạng string
    String seatNumbers;      // ✅ Danh sách ghế dạng text
    String ticketStatus;     // ✅ Trạng thái vé dạng chuỗi (BOOKED, CANCELLED, ...)
    String customerName;     // ✅ Tên khách hàng (hiển thị cho staff)

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
