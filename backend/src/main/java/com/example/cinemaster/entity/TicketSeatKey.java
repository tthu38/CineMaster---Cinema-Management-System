package com.example.cinemaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TicketSeatKey implements Serializable {
    @Column(name = "TicketID")
    private Integer ticketId;

    @Column(name = "SeatID")
    private Integer seatId;
}
