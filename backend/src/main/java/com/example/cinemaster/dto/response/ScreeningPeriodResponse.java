package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreeningPeriodResponse {

    Integer id;

    Integer movieId;
    String movieTitle;

    Integer branchId;
    String branchName;

    LocalDate startDate;
    LocalDate endDate;
    Boolean isActive;

    Integer duration;
}
