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
public class WorkScheduleCreateRequest {

    @NotNull(message = "Account ID is required")
    Integer accountId;

    @NotNull(message = "Branch ID is required")
    Integer branchId;

    @NotNull(message = "Shift date is required")
    LocalDate shiftDate;

    @NotNull(message = "Start time is required")
    LocalTime startTime;

    @NotNull(message = "End time is required")
    LocalTime endTime;

    @Size(max = 50, message = "Shift type must be at most 50 characters")
    String shiftType;

    @Size(max = 255, message = "Note must be at most 255 characters")
    String note;
}
