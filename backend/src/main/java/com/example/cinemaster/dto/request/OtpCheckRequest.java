package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCheckRequest {
    @NotBlank(message = "Mã OTP không được để trống")
    String code;
}
