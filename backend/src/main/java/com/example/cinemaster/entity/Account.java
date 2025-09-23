
        package com.example.cinemaster.entity;

import jakarta.persistence.*;
        import jakarta.validation.constraints.Size;
import lombok.*;
        import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "Accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID", nullable = false)
    Integer accountID;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Email", length = 100)
    String email;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Password")
    String password;

    @Size(max = 256)
    @Nationalized
    @Column(name = "FullName", length = 256)
    String fullName;

    @Size(max = 10)
    @Column(name = "PhoneNumber", length = 10)
    String phoneNumber;

    @Column(name = "isActive")
    Boolean isActive;

    @Column(name = "CreatedAt") // match chính xác DB
    LocalDate createdAt;

    @Size(max = 255)
    @Nationalized
    @Column(name = "GoogleAuth")
    String googleAuth;

    @Size(max = 256)
    @Nationalized
    @Column(name = "AccountAddress", length = 256)
    String address;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleID")
    Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID")
    Branch branch;

    @Column(name = "LoyaltyPoints")
    Integer loyaltyPoints;

    @Column(name = "VerificationCode", length = 255)
    private String verificationCode;

    @Column(name = "VerificationExpiry")
    private LocalDateTime verificationExpiry;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AvatarUrl")
    private String avatarUrl;
}
