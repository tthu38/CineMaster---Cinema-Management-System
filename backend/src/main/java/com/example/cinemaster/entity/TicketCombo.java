package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

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

    @Builder.Default
    @Column(name = "Quantity")
    Integer quantity = 1;  // ✅ giữ giá trị mặc định khi dùng builder

    // ✅ Tính tổng tiền combo = Combo.Price * Quantity
    @Transient
    public BigDecimal getTotalPrice() {
        if (combo == null || combo.getPrice() == null)
            return BigDecimal.ZERO;
        return combo.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
    // 🔹 Inner composite key
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
