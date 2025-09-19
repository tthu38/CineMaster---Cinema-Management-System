package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "TicketHistory")
@Table(schema = "dbo")
public class TicketHistory {

    @Id
    @NotNull
    @Column(name = "TicketHistoryID", nullable = false)
    Integer ticketHistoryID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketID", referencedColumnName = "TicketID")
    Ticket ticketID;

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
    Account changedBy;

    @Column(name = "ChangedAt")
    Instant changedAt;

    @Nationalized
    @Lob
    @Column(name = "Note")
    String note;

}