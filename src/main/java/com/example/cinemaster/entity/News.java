package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "News")
@Table(schema = "dbo")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NewsID", nullable = false)
    Integer newsID;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "Title", nullable = false)
    String title;

    @Nationalized
    @Lob
    @Column(name = "Content")
    String content;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Category", length = 50)
    String category;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ImageUrl")
    String imageUrl;

    @Column(name = "PublishDate")
    Instant publishDate;

    @ColumnDefault("0")
    @Column(name = "Views")
    Integer views;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", referencedColumnName = "AccountID")
    Account createdBy;

    @Column(name = "CreatedDate")
    Instant createdDate;

    @Column(name = "UpdatedDate")
    Instant updatedDate;

    @Nationalized
    @Lob
    @Column(name = "Remark")
    String remark;

    @ColumnDefault("1")
    @Column(name = "Active")
    Boolean active;

}