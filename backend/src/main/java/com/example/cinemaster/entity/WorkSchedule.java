package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduleID", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private Account accountID;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branchID;

    @NotNull
    @Column(name = "ShiftDate", nullable = false)
    private LocalDate shiftDate;

    @NotNull
    @Column(name = "StartTime", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "EndTime", nullable = false)
    private LocalTime endTime;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ShiftType", length = 50)
    private String shiftType;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Note")
    private String note;

}