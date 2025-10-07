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

    // PeriodID không cần trong request POST/PUT

    Integer movieId; // FK: ID của Movie
    Integer branchId; // FK: ID của Branch

    // Sử dụng String hoặc LocalDate, tùy thuộc cách bạn muốn xử lý format ngày
    LocalDate startDate;
    LocalDate endDate;
    private Boolean isActive;
}
