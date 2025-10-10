package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import java.time.*;

public record WorkScheduleCreateRequest(
        @NotNull Integer accountId,
        @NotNull Integer branchId,
        @NotNull LocalDate shiftDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 50)  String shiftType,
        @Size(max = 255) String note
) {}
