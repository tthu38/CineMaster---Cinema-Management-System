package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "TicketSeat",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"SeatID"}) // 🧩 mỗi Seat chỉ thuộc 1 Ticket
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketSeat {

    @EmbeddedId
    private TicketSeatKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "TicketID", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seatId")
    @JoinColumn(name = "SeatID", nullable = false)
    private Seat seat;

    public TicketSeat(Ticket ticket, Seat seat) {
        this.ticket = ticket;
        this.seat = seat;
        this.id = new TicketSeatKey(
                ticket != null ? ticket.getTicketId() : null,
                seat != null ? seat.getSeatID() : null
        );
    }
}
