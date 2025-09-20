package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Discount")
@Table(schema = "dbo")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiscountID", nullable = false)
    Integer discountID;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Code", length = 50, unique = true)
    String code;

    @Nationalized
    @Lob
    @Column(name = "DiscountDescription")
    String discountDescription;

    @Column(name = "PercentOff", precision = 5, scale = 2)
    BigDecimal percentOff;

    @Column(name = "FixedAmount", precision = 10, scale = 2)
    BigDecimal fixedAmount;

    @Column(name = "PointCost")
    Integer pointCost;

    @Column(name = "CreateAt")
    LocalDate createAt;

    @Column(name = "ExpiryDate")
    LocalDate expiryDate;

    @Column(name = "MaxUsage")
    Integer maxUsage;

    @Size(max = 20)
    @Nationalized
    @Column(name = "DiscountStatus", length = 20)
    String discountStatus;
}
