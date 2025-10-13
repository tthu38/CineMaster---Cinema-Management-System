// src/main/java/com/example/cinemaster/dto/request/ShowtimeUpdateRequest.java
package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeUpdateRequest(
        @NotNull Integer periodId,
        @NotNull Integer auditoriumId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotBlank @Size(max = 50) String language,
        @NotNull @DecimalMin("0.0") BigDecimal price
) {}
