package com.example.cinemaster.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BranchRequest {
    String branchName;
    String address;
    String phone;
    String email;
    Integer managerId;
    LocalTime openTime;
    LocalTime closeTime;
}
