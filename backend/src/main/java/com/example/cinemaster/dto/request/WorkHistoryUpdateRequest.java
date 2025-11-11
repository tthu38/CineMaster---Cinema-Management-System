package com.example.cinemaster.dto.request;

import com.example.cinemaster.validation.NoShiftOverlapOnUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@NoShiftOverlapOnUpdate
public record WorkHistoryUpdateRequest(
        @NotNull Integer id,
        Integer affectedAccountId,
        @Size(max = 100) String action,
        Instant actionTime,
        @Size(max = 255) String description
) {}
