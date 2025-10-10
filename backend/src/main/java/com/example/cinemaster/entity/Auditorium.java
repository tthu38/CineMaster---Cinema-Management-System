package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Entity
@Table(name = "Auditorium", schema = "dbo")
public class Auditorium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditoriumID", nullable = false)
    Integer auditoriumID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", referencedColumnName = "BranchID")
    Branch branch;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Name", length = 100)
    String name;

    @Column(name = "Capacity")
    Integer capacity;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "Type", nullable = false, length = 20)
    String type;

    @Column(name = "IsActive")
    Boolean isActive;
}
