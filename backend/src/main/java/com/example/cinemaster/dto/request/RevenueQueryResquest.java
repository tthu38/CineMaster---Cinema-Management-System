// src/main/java/com/example/cinemaster/dto/request/RevenueQuery.java
package com.example.cinemaster.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenueQueryResquest {
    private RevenueScopeResquest scope;

    // neo thời gian:
    // SHIFT & DAY dùng anchorDate (yyyy-MM-dd)
    private LocalDate anchorDate;   // ví dụ 2025-10-15 (bắt buộc với SHIFT, DAY)

    // MONTH dùng year (ví dụ 2025)
    private Integer year;           // cho scope=MONTH

    // YEAR dùng fromYear..toYear (ví dụ 2023..2025)
    private Integer fromYear;       // optional
    private Integer toYear;         // optional

    // lọc chi nhánh (Admin có thể null = tất cả; Manager/Staff bị ép chi nhánh theo user)
    private Integer branchId;
}
