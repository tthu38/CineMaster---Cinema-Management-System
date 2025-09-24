package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {
    Integer id;
    String fullName;
    String email;
    String phoneNumber;
    String address;
    String roleName;
    LocalDate createdAt;
    Integer loyaltyPoints;
    String avatarUrl;
}
