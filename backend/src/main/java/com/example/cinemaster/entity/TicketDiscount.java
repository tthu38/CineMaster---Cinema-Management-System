package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "TicketDiscount")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(TicketDiscount.TicketDiscountKey.class)
public class TicketDiscount {

    @Id
    @Column(name = "TicketID")
    Integer ticketId;

    @Id
    @Column(name = "DiscountID")
    Integer discountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", insertable = false, updatable = false)
    Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DiscountID", insertable = false, updatable = false)
    Discount discount;

    @Column(name = "Amount", precision = 10, scale = 2)
    BigDecimal amount;

    // Inner ID class
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TicketDiscountKey implements Serializable {
        Integer ticketId;
        Integer discountId;
    }
}
