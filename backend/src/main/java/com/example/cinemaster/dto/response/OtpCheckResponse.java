package com.example.cinemaster.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCheckResponse {
    String movieTitle;
    String branchName;
    String auditoriumName;
    String language;
    LocalDateTime startTime;
    LocalDateTime endTime;

    List<String> seats;
    List<String> combos;

    BigDecimal totalPrice;
    String paymentMethod;
    String ticketStatus;
}
