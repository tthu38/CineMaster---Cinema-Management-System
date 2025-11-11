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

    BigDecimal seatTotal;
    BigDecimal comboTotal;
    BigDecimal originalPrice;
    BigDecimal discountTotal;
    BigDecimal totalPrice;

    String paymentMethod;
    LocalDateTime bookingTime;
    LocalDateTime holdUntil;

    List<Integer> seatIds;
    List<ComboResponse> combos;
    List<DiscountResponse> discounts;

    String movieTitle;
    String auditoriumName;
    String seatNames;
    String branchAddress;
    LocalDateTime startTime;

    String branchName;
    String showtimeStart;
    String seatNumbers;
    String ticketStatus;
    String customerName;

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
