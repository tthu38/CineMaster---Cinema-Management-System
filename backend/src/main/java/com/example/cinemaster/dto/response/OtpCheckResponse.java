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


    // ===== Thông tin kỹ thuật =====
    private Integer showtimeId;
    private Integer auditoriumId;


    // ===== Thông tin hiển thị =====
    private String movieTitle;
    private String branchName;
    private String auditoriumName;
    private String language;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    // ===== Ghế và Combo =====
    private List<String> seats;      // Trả mã ghế: A10, B5, ...
    private List<Integer> seatIds;   // ID ghế (nếu FE cần)
    private List<String> combos;     // Combo tên


    // ===== Thanh toán =====
    private BigDecimal totalPrice;
    private String paymentMethod;
    private String ticketStatus;
}



