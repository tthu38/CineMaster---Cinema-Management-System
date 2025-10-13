package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.Size;
import java.time.*;

public record WorkScheduleUpdateRequest(
        Integer branchId,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 50)  String shiftType,
        @Size(max = 255) String note
) {}
