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
    @Column(name = "TicketID")
    Integer ticketId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", nullable = false)
    Account account;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeID", nullable = false)
    Showtime showtime;

    @Transient
    BigDecimal seatPrice;

    @Transient
    BigDecimal comboPrice;

    @Column(name = "TotalPrice", precision = 12, scale = 2)
            BigDecimal totalPrice;


    @Column(name = "BookingTime", columnDefinition = "DATETIME2(0) DEFAULT SYSDATETIME()")
    LocalDateTime bookingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "TicketStatus", length = 30, nullable = false)
    @Builder.Default
    TicketStatus ticketStatus = TicketStatus.HOLDING;


    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", length = 10, nullable = false)
    @Builder.Default
    PaymentMethod paymentMethod = PaymentMethod.CASH;


    @Column(name = "HoldUntil", columnDefinition = "DATETIME2(0)")
    LocalDateTime holdUntil;


    @Column(name = "CustomerEmail")
    String customerEmail;

    @Transient
    BigDecimal discountTotal;

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


    public enum TicketStatus {
        HOLDING, BOOKED, USED, CANCEL_REQUESTED, CANCELLED, REFUNDED
    }


    public enum PaymentMethod {
        CASH,
        ONLINE
    }
}

