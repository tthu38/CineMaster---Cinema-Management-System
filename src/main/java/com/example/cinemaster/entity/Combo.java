package com.example.cinemaster.entity;

import jakarta.persistence.*;
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
@Entity(name = "Combo")
@Table(schema = "dbo")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ComboID", nullable = false)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID")
    Branch branchID;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Name", length = 100)
    String name;

    @Column(name = "Price", precision = 10, scale = 2)
    BigDecimal price;

    @Nationalized
    @Lob
    @Column(name = "Description")
    String description;

    @Nationalized
    @Lob
    @Column(name = "Items")
    String items;

    @Column(name = "Available")
    Boolean available;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ImageURL")
    String imageURL;

}