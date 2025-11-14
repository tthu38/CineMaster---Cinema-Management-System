package com.example.cinemaster.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "ContactRequest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer contactID;

    @Column(nullable = false, length = 100)
    String fullName;

    @Column(nullable = false, length = 255)
    String email;

    @Column(length = 20)
    String phone;

    @Column(nullable = false, length = 100)
    String subject;

    @Column(nullable = false, length = 500)
    String message;

    @Column(nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    String status = "Pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HandledBy")
    Account handledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID")
    Branch branch;

    // 🟢 Thêm mới
    @Column(length = 500)
    String handleNote;

    @Column
    LocalDateTime handledAt;

    @Column(name = "IsSpam")
    private Boolean isSpam = false;

}

