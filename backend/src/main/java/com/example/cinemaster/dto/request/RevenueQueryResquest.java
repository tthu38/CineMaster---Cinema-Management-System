// src/main/java/com/example/cinemaster/dto/request/RevenueQuery.java
package com.example.cinemaster.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenueQueryResquest {
    private RevenueScopeResquest scope;

    private LocalDate anchorDate;

    private Integer year;

    private Integer fromYear;
    private Integer toYear;

    private Integer branchId;
}
