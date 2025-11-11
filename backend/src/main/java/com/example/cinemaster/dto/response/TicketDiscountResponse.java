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

    Integer ticketId;
    String discountCode;
    BigDecimal discountAmount;
    BigDecimal newTotal;
    BigDecimal originalTotal;

    BigDecimal seatPrice;
    BigDecimal comboPrice;
}
