package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "TicketSeat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(TicketSeat.TicketSeatKey.class)
public class TicketSeat {

    @Id
    @Column(name = "TicketID")
    Integer ticketId;

    @Id
    @Column(name = "SeatID")
    Integer seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", insertable = false, updatable = false)
    Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SeatID", insertable = false, updatable = false)
    Seat seat;

    // ----- Inner class làm khóa kép -----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TicketSeatKey implements java.io.Serializable {
        Integer ticketId;
        Integer seatId;
    }
}
