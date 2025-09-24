package com.example.cinemaster.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String avatarUrl; // giữ sẵn, nếu bạn muốn update trực tiếp URL
}
