package com.example.cinemaster.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {
    private Integer ticketId;

    // Thông tin phim
    private String movieTitle;
    private String movieGenre;
    private Integer movieDuration;

    // Thông tin rạp & suất chiếu
    private String branchName;
    private String auditoriumName;
    private String showtimeStart;
    private String showtimeEnd;

    // Thông tin vé
    private String seatNumbers;
    private String ticketStatus;
    private Double totalPrice;
    private String paymentMethod;

    // Combo
    private Object comboList; // hoặc List<TicketComboResponse> nếu bạn đã có DTO riêng
}
