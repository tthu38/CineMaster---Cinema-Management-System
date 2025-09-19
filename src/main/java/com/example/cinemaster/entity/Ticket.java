package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Ticket")
@Table(schema = "dbo")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID", nullable = false)
    Integer ticketID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", referencedColumnName = "AccountID")
    Account accountID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeID", referencedColumnName = "ShowtimeID")
    Showtime showtimeID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SeatID", referencedColumnName = "SeatID")
    Seat seatID;

    @Column(name = "TotalPrice", precision = 10, scale = 2)
    BigDecimal totalPrice;

    @Column(name = "BookingTime")
    Instant bookingTime;

    @Size(max = 20)
    @Nationalized
    @Column(name = "Status", length = 20)
    String status;

    @Size(max = 20)
    @Nationalized
    @Column(name = "PaymentMethod", length = 20)
    String paymentMethod;

}