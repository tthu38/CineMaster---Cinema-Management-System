package com.example.cinemaster.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;

    private String email;

    private String phoneNumber;
    private String address;
    private String avatarUrl; // giữ sẵn, nếu bạn muốn update trực tiếp URL
}
