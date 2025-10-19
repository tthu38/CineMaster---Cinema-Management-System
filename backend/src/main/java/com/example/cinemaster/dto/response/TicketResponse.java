package com.example.cinemaster.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Integer ticketId;        // ✅ Quan trọng nhất
    private String movieTitle;
    private String branchName;
    private String showtimeStart;
    private String seatNumbers;
    private String ticketStatus;
    private Double totalPrice;
    private String paymentMethod;
    private String customerName;     // ✅ Thêm để staff hiển thị khách
}
