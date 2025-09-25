package com.example.cinemaster.dto.request;

import lombok.Data;

@Data
public class EmailRequest {
    private String email;
    private String otp; // dùng cho verify
}
