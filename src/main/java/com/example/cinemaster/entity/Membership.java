package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Membership")
@Table(schema = "dbo")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MembershipID", nullable = false)
    Integer membershipID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", referencedColumnName = "AccountID")
    Account accountID;

    @Size(max = 20)
    @Nationalized
    @Column(name = "\"Level\"", length = 20)
    String level;

    @Column(name = "Points")
    Integer points;

    @Column(name = "JoinDate")
    LocalDate joinDate;

    @Column(name = "ExpiryDate")
    LocalDate expiryDate;

}