package com.example.cinemaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class TicketDiscountId implements Serializable {
    private static final long serialVersionUID = -2946662498182783055L;
    @NotNull
    @Column(name = "TicketID", nullable = false)
    Integer ticketID;

    @NotNull
    @Column(name = "DiscountID", nullable = false)
    Integer discountID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TicketDiscountId entity = (TicketDiscountId) o;
        return Objects.equals(this.discountID, entity.discountID) &&
                Objects.equals(this.ticketID, entity.ticketID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discountID, ticketID);
    }

}