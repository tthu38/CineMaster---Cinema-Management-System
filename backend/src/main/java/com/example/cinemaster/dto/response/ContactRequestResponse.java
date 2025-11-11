package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactRequestResponse {
    Integer contactID;
    String fullName;
    String email;
    String phone;
    String subject;
    String message;
    String status;
    LocalDateTime createdAt;

    String handledBy;
    String branchName;
    String handleNote;
    LocalDateTime handledAt;
}
