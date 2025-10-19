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

    String handledBy;   // tên nhân viên xử lý
    String branchName;  // tên chi nhánh
    String handleNote;  // ghi chú xử lý
    LocalDateTime handledAt; // thời điểm xử lý
}
