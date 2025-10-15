package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketBookingRequest {

    @NotNull(message = "AccountID không được để trống")
    Integer accountId;

    @NotNull(message = "ShowtimeID không được để trống")
    Integer showtimeId;

    @NotNull(message = "Danh sách ghế không được để trống")
    List<Integer> seatIds;

    Integer comboId;          // có thể null
    String discountCode;      // có thể null

    @NotNull(message = "Phương thức thanh toán là bắt buộc")
    String paymentMethod;     // Cash / Card / Momo
}
