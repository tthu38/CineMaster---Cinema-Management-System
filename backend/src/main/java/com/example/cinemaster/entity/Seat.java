package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Seat")
@Table(schema = "dbo")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SeatID", nullable = false)
    Integer seatID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AuditoriumID", referencedColumnName = "AuditoriumID")
    Auditorium auditorium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TypeID", referencedColumnName = "TypeID")
    SeatType seatType;

    @Size(max = 10)
    @Nationalized
    @Column(name = "SeatNumber", length = 10)
    String seatNumber;

    @Size(max = 10)
    @Nationalized
    @Column(name = "SeatRow", length = 10)
    String seatRow;

    @Column(name = "ColumnNumber")
    Integer columnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    SeatStatus status;
    public enum SeatStatus {
        AVAILABLE,
        BROKEN,
        RESERVED,
    }
}