package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "TicketSeat", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketSeat {

    @EmbeddedId
    TicketSeatId id;

    @MapsId("ticketID")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", nullable = false)
    Ticket ticket;

    @MapsId("seatID")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SeatID", nullable = false)
    Seat seat;
}
