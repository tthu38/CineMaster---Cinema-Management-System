package com.example.cinemaster.dto.request;

import com.example.cinemaster.validation.NoShiftOverlapOnUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@NoShiftOverlapOnUpdate // <— check trùng ca khi sửa
public record WorkHistoryUpdateRequest(
        @NotNull Integer id,                        // để validator loại trừ chính nó
        Integer affectedAccountId,
        @Size(max = 100) String action,            // có thể null (không đổi)
        Instant actionTime,                        // có thể null (không đổi)
        @Size(max = 255) String description
) {}
