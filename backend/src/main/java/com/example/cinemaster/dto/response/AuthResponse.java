package com.example.cinemaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    private String email;
    private String fullName;
    private String role;

    // ✅ Thêm 2 field này để Manager có chi nhánh
    private Integer branchId;
    private String branchName;
}
