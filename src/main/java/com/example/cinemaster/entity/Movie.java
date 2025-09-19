package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Movie")
@Table(schema = "dbo")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovieID", nullable = false)
    Integer movieID;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "Title", nullable = false)
    String title;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "Genre", nullable = false, length = 100)
    String genre;

    @NotNull
    @Column(name = "Duration", nullable = false)
    Integer duration;

    @NotNull
    @Column(name = "ReleaseDate", nullable = false)
    LocalDate releaseDate;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "Director", nullable = false)
    String director;

    @NotNull
    @Nationalized
    @Lob
    @Column(name = "\"Cast\"", nullable = false)
    String cast;

    @Size(max = 100)
    @Nationalized
    @Column(name = "\"Language\"", length = 100)
    String language;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "AgeRestriction", nullable = false, length = 20)
    String ageRestriction;

    @Nationalized
    @Lob
    @Column(name = "Description")
    String description;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "TrailerUrl", nullable = false)
    String trailerUrl;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "PosterUrl", nullable = false)
    String posterUrl;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Country", length = 100)
    String country;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "Status", nullable = false, length = 20)
    String status;

}