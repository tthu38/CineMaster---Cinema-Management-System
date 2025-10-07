package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
public class MembershipLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LevelID", nullable = false)
    private Integer id;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "LevelName", nullable = false, length = 20)
    private String levelName;

    @NotNull
    @Column(name = "MinPoints", nullable = false)
    private Integer minPoints;

    @NotNull
    @Column(name = "MaxPoints", nullable = false)
    private Integer maxPoints;

    @Nationalized
    @Lob
    @Column(name = "Benefits")
    private String benefits;

}