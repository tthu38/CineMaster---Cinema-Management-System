package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "WorkHistory")
@Table(schema = "dbo")
public class WorkHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WorkHistoryID", nullable = false)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID")
    Account accountID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AffectedAccountID")
    Account affectedAccountID;

    @Size(max = 100)
    @Nationalized
    @Column(name = "WKAction", length = 100)
    String action;

    @Column(name = "ActionTime")
    Instant actionTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WKDescription")
    String description;

}