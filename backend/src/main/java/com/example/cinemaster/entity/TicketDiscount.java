package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "TicketDiscount", schema = "dbo")
public class TicketDiscount {

    @EmbeddedId
    TicketDiscountId id;

    @MapsId("ticketID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TicketID", nullable = false)
    Ticket ticketID;

    @MapsId("discountID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DiscountID", nullable = false)
    Discount discountID;

    @Column(name = "Amount", precision = 10, scale = 2)
    BigDecimal amount;
}
