
package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ShowtimeCreateRequest.java
public record ShowtimeCreateRequest(
        @NotNull Integer periodId,
        @NotNull Integer auditoriumId,
        @NotNull @FutureOrPresent LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        @NotBlank
        @Pattern(regexp = "^(Vietnamese|English)$", message = "language must be Vietnamese or English")
        String language,
        @NotNull @DecimalMin(value = "0.0") BigDecimal price
) {}

