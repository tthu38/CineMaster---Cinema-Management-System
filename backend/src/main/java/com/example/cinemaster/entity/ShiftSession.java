package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ShiftSession")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", nullable = false)
    Account staff;

    @Column(name = "StartTime")
    LocalDateTime startTime;

    @Column(name = "EndTime")
    LocalDateTime endTime;

    @Column(name = "OpeningCash", precision = 12, scale = 2)
    BigDecimal openingCash;

    @Column(name = "ClosingCash", precision = 12, scale = 2)
    BigDecimal closingCash;

    @Column(name = "ExpectedCash", precision = 12, scale = 2)
    BigDecimal expectedCash;

    @Column(name = "Difference", precision = 12, scale = 2)
    BigDecimal difference;

    @Column(name = "Status", length = 20)
    String status;
}
