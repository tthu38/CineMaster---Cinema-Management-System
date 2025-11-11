package com.example.cinemaster.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {
    private Integer ticketId;

    private String customerName;

    private String movieTitle;
    private String movieGenre;
    private Integer movieDuration;

    private String branchName;
    private String auditoriumName;
    private String showtimeStart;
    private String showtimeEnd;

    private String seatNumbers;
    private String ticketStatus;
    private Double totalPrice;
    private String paymentMethod;

    private Object comboList;
}
