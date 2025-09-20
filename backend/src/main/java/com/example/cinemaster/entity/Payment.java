package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Payment")
@Table(schema = "dbo")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    Integer id;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "OrderCode", nullable = false, length = 100)
    String orderCode;

    @NotNull
    @Column(name = "Amount", nullable = false, precision = 10, scale = 2)
    BigDecimal amount;

    @Nationalized
    @Lob
    @Column(name = "Description")
    String description;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "Status", nullable = false, length = 20)
    String status;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CheckoutUrl")
    String checkoutUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", referencedColumnName = "TicketID")
    Ticket ticketID;

    @Column(name = "CreatedAt")
    Instant createdAt;

    @Column(name = "UpdatedAt")
    Instant updatedAt;

}