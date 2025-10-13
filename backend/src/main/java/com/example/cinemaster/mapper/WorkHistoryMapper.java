package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.WorkHistoryResponse;
import com.example.cinemaster.entity.WorkHistory;

public final class WorkHistoryMapper {
    private WorkHistoryMapper() {}

    public static WorkHistoryResponse toResponse(WorkHistory e) {
        return new WorkHistoryResponse(
                e.getId(),
                e.getAccountID() != null ? e.getAccountID().getAccountID() : null,
                e.getAffectedAccountID() != null ? e.getAffectedAccountID().getAccountID() : null,
                e.getAction(),
                e.getActionTime(),
                e.getDescription()
        );
    }
}
