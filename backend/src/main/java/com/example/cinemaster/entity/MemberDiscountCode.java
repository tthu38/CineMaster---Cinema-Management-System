package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "MemberDiscountCode")
@Table(schema = "dbo")
public class MemberDiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RedeemedID", nullable = false)
    Integer redeemedID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MembershipID", referencedColumnName = "MembershipID")
    Membership membershipID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DiscountID", referencedColumnName = "DiscountID")
    Discount discountID;

    @Column(name = "RedeemedDate")
    Instant redeemedDate;

    @Column(name = "Used")
    Boolean used;

}