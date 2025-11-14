package com.example.cinemaster.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "ShiftRequest")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftRequest {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestID;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", nullable = false)
    private Account account;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;


    @Column(name = "ShiftDate", nullable = false)
    private LocalDate shiftDate;


    @Column(name = "ShiftType", nullable = false, length = 50)
    private String shiftType; // MORNING, AFTERNOON, NIGHT


    @Column(name = "Status", length = 20)
    private String status = "PENDING"; // PENDING / APPROVED / REJECTED


    @Column(name = "Note", length = 255)
    private String note;


    @Column(name = "CreatedAt", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime createdAt;
}



