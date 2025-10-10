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
public class NewsDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetailID", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NewsID", nullable = false)
    private News newsID;

    @Size(max = 255)
    @Nationalized
    @Column(name = "SectionTitle")
    private String sectionTitle;

    @Nationalized
    @Lob
    @Column(name = "SectionContent")
    private String sectionContent;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(name = "DisplayOrder")
    private Integer displayOrder;

}