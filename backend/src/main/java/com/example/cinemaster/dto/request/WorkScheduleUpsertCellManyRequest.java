package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleUpsertCellManyRequest {

    @NotNull(message = "Cần phải có ID chi nhánh")
    Integer branchId;

    @NotNull(message = "Thời gian là bắt buộc")
    LocalDate date;

    @NotBlank(message = "Shift type is required")
    @Size(max = 50, message = "Loại ca phải dài tối đa 50 ký tự")
    String shiftType; // MORNING / AFTERNOON / NIGHT

    @NotNull(message = "Danh sách tài khoản không thể rỗng")
    @Size(min = 0, message = "Danh sách tài khoản không thể rỗng")
    List<Integer> accountIds;

    LocalTime startTime;
    LocalTime endTime;
}
