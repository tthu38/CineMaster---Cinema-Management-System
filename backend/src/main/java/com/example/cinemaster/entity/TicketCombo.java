package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "TicketCombo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(TicketCombo.TicketComboKey.class)
public class TicketCombo {

    @Id
    @Column(name = "TicketID")
    Integer ticketId;

    @Id
    @Column(name = "ComboID")
    Integer comboId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", insertable = false, updatable = false)
    Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComboID", insertable = false, updatable = false)
    Combo combo;

    @Column(name = "Quantity")
    Integer quantity = 1;

    // Inner composite key
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TicketComboKey implements java.io.Serializable {
        Integer ticketId;
        Integer comboId;
    }
}
