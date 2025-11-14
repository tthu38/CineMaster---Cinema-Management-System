// src/main/java/com/example/cinemaster/entity/Showtime.java
package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Showtime")
@Table(name = "Showtimes", schema = "dbo")
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShowtimeID", nullable = false)
    Integer showtimeID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PeriodID", referencedColumnName = "PeriodID")
    ScreeningPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AuditoriumID", referencedColumnName = "AuditoriumID")
    Auditorium auditorium;

    @Column(name = "StartTime")
    LocalDateTime startTime;

    @Column(name = "EndTime")
    LocalDateTime endTime;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Languages", length = 50)
    String language;

    @Column(name = "Price", precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "Status", length = 20)
    String status = "ACTIVE";

//    @Transient
    public Movie getMovie() {
        return period != null ? period.getMovie() : null;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
