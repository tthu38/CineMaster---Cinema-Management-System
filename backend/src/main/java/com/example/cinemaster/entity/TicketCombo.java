package com.example.cinemaster.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.math.BigDecimal;


@Entity
@Table(name = "TicketCombo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketCombo implements Serializable {


    @EmbeddedId
    private TicketComboKey id;


    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "TicketID")
    private Ticket ticket;


    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("comboId")
    @JoinColumn(name = "ComboID")
    private Combo combo;


    @Column(name = "Quantity")
    @Builder.Default
    private Integer quantity = 1;


    @Transient
    public BigDecimal getTotalPrice() {
        if (combo == null || combo.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return combo.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}

