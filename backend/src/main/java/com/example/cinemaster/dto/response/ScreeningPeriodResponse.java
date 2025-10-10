package com.example.cinemaster.dto.response;

import java.time.LocalDate;

// dto/response/ScreeningPeriodLite.java
public record ScreeningPeriodResponse(
        Integer periodId,
        Integer movieId,
        String movieTitle,
        Integer branchId,
        LocalDate startDate,
        LocalDate endDate,
        Integer duration
) {}
