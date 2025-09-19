package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.LocalTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "Branchs") // match table trong DB
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BranchID", nullable = false)
    Integer id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "BranchName")
    String branchName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address")
    String address;

    @Size(max = 10)
    @Column(name = "Phone", length = 10)
    String phone;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Email", length = 100)
    String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ManagerID", referencedColumnName = "AccountID")
    Account manager;

    @Column(name = "OpenTime")
    LocalTime openTime;

    @Column(name = "CloseTime")
    LocalTime closeTime;
}
