package com.example.cinemaster.dto.response;

import java.time.*;

public record WorkScheduleResponse(
        Integer id,
        Integer accountId,
        String  accountName,
        Integer branchId,
        String  branchName,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        String shiftType,
        String note
) {}
