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

    // ================== THÔNG TIN PHIM & SUẤT CHIẾU ==================
    String movieTitle;
    String branchName;
    String auditoriumName;
    String showDate;      // yyyy-MM-dd
    String showTime;      // HH:mm

    // ================== GHẾ & COMBO ==================
    List<String> seats;   // VD: ["A5", "A6", "A7"]
    String comboName;
    BigDecimal comboPrice;

    // ================== THANH TOÁN ==================
    BigDecimal totalPrice;
    String paymentMethod;
    String ticketStatus;
    LocalDateTime bookingTime;

    // ================== ƯU ĐÃI ==================
    String discountCode;
    BigDecimal discountAmount;
}
