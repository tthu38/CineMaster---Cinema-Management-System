package com.example.cinemaster.dto.request;

import com.example.cinemaster.validation.NoShiftOverlap;
import jakarta.validation.constraints.*;
import java.time.Instant;

@NoShiftOverlap // <— check trùng ca khi tạo
public record WorkHistoryCreateRequest(
        @NotNull Integer accountId,
        Integer affectedAccountId,
        @NotBlank @Size(max = 100) String action,   // MORNING/AFTERNOON/NIGHT
        @NotNull Instant actionTime,                // thời điểm bắt đầu (trong ngày)
        @Size(max = 255) String description
) {}
