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

    @NotNull(message = "Branch ID is required")
    Integer branchId;

    @NotNull(message = "Date is required")
    LocalDate date;

    @NotBlank(message = "Shift type is required")
    @Size(max = 50, message = "Shift type must be at most 50 characters")
    String shiftType; // MORNING / AFTERNOON / NIGHT

    @NotNull(message = "Account list cannot be null")
    @Size(min = 0, message = "Account list cannot be empty")
    List<Integer> accountIds;

    LocalTime startTime;
    LocalTime endTime;
}
