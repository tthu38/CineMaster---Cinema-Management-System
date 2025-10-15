package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "Seat", schema = "dbo")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SeatID", nullable = false)
    Integer seatID;

    // ==================== QUAN HỆ ====================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AuditoriumID", referencedColumnName = "AuditoriumID", nullable = false)
    Auditorium auditorium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TypeID", referencedColumnName = "TypeID", nullable = false)
    SeatType seatType;

    // ==================== THÔNG TIN GHẾ ====================
    @Size(max = 10)
    @Nationalized
    @Column(name = "SeatNumber", length = 10, nullable = false)
    String seatNumber;

    @Size(max = 10)
    @Nationalized
    @Column(name = "SeatRow", length = 10)
    String seatRow;

    @Column(name = "ColumnNumber")
    Integer columnNumber;

    // ==================== TRẠNG THÁI GHẾ ====================
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20, nullable = false)
    SeatStatus status;

    @Column(name = "LockedUntil")
    LocalDateTime lockedUntil;

    // ==================== ENUM ====================
    public enum SeatStatus {
        AVAILABLE,  // Ghế trống
        BROKEN,     // Ghế bị hỏng
        RESERVED,   // Ghế đang được giữ
        BOOKED      // Ghế đã được đặt vé (thanh toán xong)
    }
}
