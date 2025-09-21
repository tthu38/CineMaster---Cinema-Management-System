package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.example.cinemaster.entity.Account}
 */
@Value
public class UserProfileUpdateRequest implements Serializable {
    @Size(max = 256, message = "Full name cannot exceed 256 characters")
    String fullName;
    @Size(max = 256, message = "Address cannot exceed 256 characters")
    String address;
}