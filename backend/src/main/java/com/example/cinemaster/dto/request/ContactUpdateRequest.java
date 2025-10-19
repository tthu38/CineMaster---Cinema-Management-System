package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactUpdateRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    String status; // Pending / Processing / Resolved / Rejected

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    String handleNote;
}
