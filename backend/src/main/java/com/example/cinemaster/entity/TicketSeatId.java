package com.example.cinemaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class TicketSeatId implements Serializable {
    @Column(name = "TicketID", nullable = false)
    Integer ticketID;

    @Column(name = "SeatID", nullable = false)
    Integer seatID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketSeatId that = (TicketSeatId) o;
        return Objects.equals(ticketID, that.ticketID)
                && Objects.equals(seatID, that.seatID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketID, seatID);
    }
}

