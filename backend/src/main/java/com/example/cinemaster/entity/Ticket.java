package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    Integer ticketID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", nullable = false)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeID", nullable = false)
    Showtime showtime;

    @Column(name = "TotalPrice", precision = 12, scale = 2)
    BigDecimal totalPrice;

    @Column(name = "BookingTime")
    LocalDateTime bookingTime;

    @Column(name = "TicketStatus", length = 30)
    String ticketStatus; // Booked, Used, Cancelled, Refunded, CancelRequested

    @Column(name = "PaymentMethod", length = 20)
    String paymentMethod;

    // ================= RELATIONSHIPS =================
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TicketSeat> ticketSeats;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TicketCombo> ticketCombos;
}
