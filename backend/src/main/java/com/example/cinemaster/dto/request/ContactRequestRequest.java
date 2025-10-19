package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactRequestRequest {

    @NotBlank(message = "Họ tên không được để trống")
    String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email là bắt buộc")
    String email;

    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    String phone;

    @NotBlank(message = "Vui lòng chọn chủ đề liên hệ")
    String subject;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 500, message = "Nội dung tối đa 500 ký tự")
    String message;

    @Positive(message = "Chi nhánh không hợp lệ")
    Integer branchId; // 🟢 chi nhánh khách chọn
}
