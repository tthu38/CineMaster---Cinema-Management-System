package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "Ticket", schema = "dbo")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID", nullable = false)
    Integer ticketID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", referencedColumnName = "AccountID")
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeID", referencedColumnName = "ShowtimeID")
    Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SeatID", referencedColumnName = "SeatID")
    Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComboID", referencedColumnName = "ComboID")
    Combo combo;

    @Column(name = "TotalPrice", precision = 10, scale = 2)
    BigDecimal totalPrice;

    @Column(name = "BookingTime")
    LocalDateTime bookingTime;

    @Size(max = 20)
    @Nationalized
    @Column(name = "TicketStatus", length = 20)
    String ticketStatus;

    @Size(max = 20)
    @Nationalized
    @Column(name = "PaymentMethod", length = 20)
    String paymentMethod;
}
