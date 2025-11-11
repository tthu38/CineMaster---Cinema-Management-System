package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleUpdateRequest {

    @NotNull(message = "ID tài khoản là bắt buộc")
    Integer accountId;

    @NotNull(message = "Cần phải có ID chi nhánh")
    Integer branchId;

    @NotNull(message = "Ngày mở ca làm là bắt buộc")
    LocalDate shiftDate;

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    LocalTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    LocalTime endTime;

    @Size(max = 50, message = "Loại ca phải dài tối đa 50 ký tự")
    String shiftType;

    @Size(max = 255, message = "Ghi chú phải có tối đa 255 ký tự")
    String note;
}
