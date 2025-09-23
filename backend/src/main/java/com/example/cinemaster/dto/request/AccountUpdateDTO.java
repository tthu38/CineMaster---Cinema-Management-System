package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountUpdateDTO {
    @NotBlank(message = "Full name cannot be empty")
    @Size(max = 256, message = "Full name cannot exceed 256 characters")
    String fullName;

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "0[0-9]{9}", message = "Phone number must start with 0 and have 10 digits")
    String phoneNumber;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    String email;

    @Size(max = 256, message = "Address cannot exceed 256 characters")
    String address;
}