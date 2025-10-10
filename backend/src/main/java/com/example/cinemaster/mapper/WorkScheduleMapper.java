package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.WorkScheduleResponse;
import com.example.cinemaster.entity.WorkSchedule;

public class WorkScheduleMapper {
    public static WorkScheduleResponse toResponse(WorkSchedule e) {
        return new WorkScheduleResponse(
                e.getId(),
                e.getAccountID() != null ? e.getAccountID().getAccountID() : null,
                e.getAccountID() != null ? e.getAccountID().getFullName() : null,
                e.getBranchID() != null ? e.getBranchID().getId() : null,
                e.getBranchID() != null ? e.getBranchID().getBranchName() : null,
                e.getShiftDate(),
                e.getStartTime(),
                e.getEndTime(),
                e.getShiftType(),
                e.getNote()
        );
    }
}
