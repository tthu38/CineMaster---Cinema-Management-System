package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "TicketHistory")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketHistoryID")
    Integer ticketHistoryID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", referencedColumnName = "TicketID")
    private Ticket ticket;



    @Size(max = 50)
    @Nationalized
    @Column(name = "OldStatus", length = 50)
    String oldStatus;

    @Size(max = 50)
    @Nationalized
    @Column(name = "NewStatus", length = 50)
    String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChangedBy", referencedColumnName = "AccountID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    Account changedBy;

    @Column(name = "ChangedAt")
    Instant changedAt;

    @Nationalized
    @Lob
    @Column(name = "Note")
    String note;
}
