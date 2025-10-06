package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountRequest {
    String email;
    String password;
    String fullName;
    String phoneNumber;
    String address;
    Integer roleId;
    Integer branchId;
}
