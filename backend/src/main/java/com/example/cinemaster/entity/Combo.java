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
@Entity
@Table(name = "Combo")
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ComboID", nullable = false)
    Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BranchID", referencedColumnName = "BranchID")
    Branch branchID;

    @Size(max = 100)
    @Nationalized
    @Column(name = "NameCombo", length = 100)
    String nameCombo;

    @Column(name = "Price", precision = 10, scale = 2)
    BigDecimal price;

    @Nationalized
    @Lob
    @Column(name = "DescriptionCombo")
    String descriptionCombo;

    @Nationalized
    @Lob
    @Column(name = "Items")
    String items;

    @Column(name = "Available")
    Boolean available;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ImageURL", length = 255)
    String imageURL;
}
