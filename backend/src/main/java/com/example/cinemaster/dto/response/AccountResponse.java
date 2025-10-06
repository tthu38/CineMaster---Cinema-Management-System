package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    Integer accountID;
    String email;
    String fullName;
    String phoneNumber;
    String address;
    Boolean isActive;
    String avatarUrl;

    // ✅ thêm để load role/branch
    Integer roleId;
    String roleName;
    Integer branchId;
    String branchName;
}


