package com.example.cinemaster.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "Membership", schema = "dbo")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MembershipID", nullable = false)
    Integer membershipID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", referencedColumnName = "AccountID")
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LevelID", referencedColumnName = "LevelID")
    MembershipLevel level;

    @Column(name = "Points")
    Integer points;

    @Column(name = "JoinDate")
    LocalDate joinDate;

    @Column(name = "ExpiryDate")
    LocalDate expiryDate;
}
