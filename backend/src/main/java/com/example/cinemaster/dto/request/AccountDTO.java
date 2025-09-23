package com.example.cinemaster.dto.request;

import com.example.cinemaster.entity.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountDTO {
    Integer accountID;
    String email;
    String fullName;
    String phoneNumber;
    Boolean isActive;
    LocalDate createdAt;
    String googleAuth;
    String address;
    Role role; // Or a RoleDTO if you want to be more granular
    Integer branchID;
    Integer loyaltyPoints;
    String avatarUrl;
    // Do not include password here for security reasons when sending to client
}