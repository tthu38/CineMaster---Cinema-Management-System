package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "SeatType")
@Table(schema = "dbo")
public class SeatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TypeID", nullable = false)
    Integer typeID;

    @Size(max = 20)
    @Nationalized
    @Column(name = "TypeName", length = 20)
    String typeName;

    @Column(name = "PriceMultiplier", precision = 10, scale = 2)
    BigDecimal priceMultiplier;

}