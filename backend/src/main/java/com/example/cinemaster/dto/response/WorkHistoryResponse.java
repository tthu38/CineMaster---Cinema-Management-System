package com.example.cinemaster.dto.response;

import java.time.Instant;

public record WorkHistoryResponse(
        Integer id,
        Integer accountId,
        Integer affectedAccountId,
        String action,
        Instant actionTime,
        String description
) {}
