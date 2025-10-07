package com.example.cinemaster.dto.response;

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
public class BranchResponse {
    Integer branchId;
    String branchName;
    String address;
    String phone;
    String email;
    Integer managerId; // Trả về ManagerID (Integer)
    String managerName; // Thêm tên Manager để tiện theo dõi
    LocalTime openTime;
    LocalTime closeTime;
    Boolean isActive;
}
