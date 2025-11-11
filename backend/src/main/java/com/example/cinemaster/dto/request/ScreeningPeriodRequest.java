package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreeningPeriodRequest {

    Integer movieId;
    Integer branchId;

    LocalDate startDate;
    LocalDate endDate;
    private Boolean isActive;
}
