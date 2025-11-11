package com.example.cinemaster.dto.request;

import com.example.cinemaster.validation.NoShiftOverlap;
import jakarta.validation.constraints.*;
import java.time.Instant;

@NoShiftOverlap
public record WorkHistoryCreateRequest(
        @NotNull Integer accountId,
        Integer affectedAccountId,
        @NotBlank @Size(max = 100) String action,
        @NotNull Instant actionTime,
        @Size(max = 255) String description
) {}
