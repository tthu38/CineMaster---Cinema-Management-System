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
    @Column(precision = 12, scale = 2)
    BigDecimal totalPrice;

    @Column(columnDefinition = "DATETIME2(0) DEFAULT SYSDATETIME()")
    LocalDateTime bookingTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    TicketStatus ticketStatus = TicketStatus.HOLDING;

    @Column(length = 20, nullable = false)
    @Builder.Default
    String paymentMethod = "Cash";

    @Column(name = "HoldUntil", columnDefinition = "DATETIME2(0)")
    LocalDateTime holdUntil;

    @Column(name = "CustomerEmail")
    String customerEmail;

    // 🎟️ Tổng giảm giá (chỉ tạm hiển thị, không lưu DB)
    @Transient
    BigDecimal discountTotal;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<TicketSeat> ticketSeats = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<TicketDiscount> ticketDiscounts = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<TicketCombo> ticketCombos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (bookingTime == null) {
            bookingTime = LocalDateTime.now();
        }
    }

    public enum TicketStatus {
        HOLDING, BOOKED, USED, CANCEL_REQUESTED, CANCELLED, REFUNDED
    }
}
