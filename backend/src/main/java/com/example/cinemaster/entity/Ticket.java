package com.example.cinemaster.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "Ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ticket {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID") // ✅ thêm từ file 2
    Integer ticketId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", nullable = false)
    Account account;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeID", nullable = false)
    Showtime showtime;


    // 💺 Tổng tiền ghế (chỉ dùng tạm trong logic, không lưu DB)
    @Transient
    BigDecimal seatPrice;


    // 🍿 Tổng tiền combo (chỉ dùng tạm trong logic, không lưu DB)
    @Transient
    BigDecimal comboPrice;


    // 💳 Tổng tiền sau giảm giá (lưu vào DB)
    @Column(name = "TotalPrice", precision = 12, scale = 2) // ✅ thêm annotation name
            BigDecimal totalPrice;


    @Column(name = "BookingTime", columnDefinition = "DATETIME2(0) DEFAULT SYSDATETIME()") // ✅ thêm name
    LocalDateTime bookingTime;


    // ⚙️ Trạng thái vé
    @Enumerated(EnumType.STRING)
    @Column(name = "TicketStatus", length = 30, nullable = false)
    @Builder.Default
    TicketStatus ticketStatus = TicketStatus.HOLDING;


    // 💵 Phương thức thanh toán (CASH / ONLINE)
    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", length = 10, nullable = false)
    @Builder.Default
    PaymentMethod paymentMethod = PaymentMethod.CASH;


    @Column(name = "HoldUntil", columnDefinition = "DATETIME2(0)")
    LocalDateTime holdUntil;


    @Column(name = "CustomerEmail")
    String customerEmail;


    // 🎟️ Tổng giảm giá (chỉ tạm hiển thị, không lưu DB)
    @Transient
    BigDecimal discountTotal;


    // ================= RELATIONSHIPS =================
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<TicketSeat> ticketSeats = new ArrayList<>();


    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<TicketDiscount> ticketDiscounts = new ArrayList<>();


    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<TicketCombo> ticketCombos = new ArrayList<>();




    @PrePersist
    void onCreate() {
        if (bookingTime == null) {
            bookingTime = LocalDateTime.now();
        }
    }




    // ================= ENUM =================
    public enum TicketStatus {
        HOLDING, BOOKED, USED, CANCEL_REQUESTED, CANCELLED, REFUNDED
    }


    public enum PaymentMethod {
        CASH,    // 💵 Thanh toán trực tiếp tại quầy
        ONLINE   // 💳 Thanh toán trực tuyến (VNPAY, MOMO, SEPAY, ...)
    }
}

