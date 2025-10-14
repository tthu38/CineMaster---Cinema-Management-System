package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.*;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.*;
import org.mapstruct.*;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = LocalTime.class)
public interface WorkScheduleMapper {

    /* ========== RESPONSE ========== */
    @Mapping(target = "accountId", source = "account.accountID")
    @Mapping(target = "accountName", source = "account.fullName")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.branchName")
    WorkScheduleResponse toResponse(WorkSchedule entity);

    /* ========== CREATE ========== */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", source = "account")
    @Mapping(target = "branch", source = "branch")
    @Mapping(target = "shiftDate", source = "req.shiftDate")
    @Mapping(target = "startTime", source = "req.startTime")
    @Mapping(target = "endTime", source = "req.endTime")
    @Mapping(target = "shiftType", source = "req.shiftType")
    @Mapping(target = "note", source = "req.note")
    WorkSchedule toEntity(WorkScheduleCreateRequest req, Account account, Branch branch);

    /* ========== UPDATE ========== */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(WorkScheduleUpdateRequest req, @MappingTarget WorkSchedule entity);

    /* ========== UPSERT CELL ========== */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", source = "account")
    @Mapping(target = "branch", source = "branch")
    @Mapping(target = "shiftDate", source = "req.date")
    @Mapping(target = "shiftType", source = "req.shiftType")
    @Mapping(target = "startTime", expression = "java(times[0])")
    @Mapping(target = "endTime", expression = "java(times[1])")
    @Mapping(target = "note", ignore = true)
    WorkSchedule toEntityForUpsert(Account account, Branch branch,
                                   WorkScheduleUpsertCellManyRequest req, LocalTime[] times);

    /* ========== CELL ASSIGNMENT ========== */
    @Mapping(target = "scheduleId", source = "id")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "accountId", source = "account.accountID")
    @Mapping(target = "accountName", source = "account.fullName")
    WorkScheduleCellAssignmentResponse toCellAssignment(WorkSchedule ws);

    /* ========== MATRIX VIEW ========== */
    default Map<String, Map<String, List<WorkScheduleCellAssignmentResponse>>> toMatrix(List<WorkSchedule> rows) {
        if (rows == null) return Collections.emptyMap();
        return rows.stream().collect(Collectors.groupingBy(
                ws -> ws.getShiftDate().toString(),
                Collectors.groupingBy(
                        WorkSchedule::getShiftType,
                        Collectors.mapping(this::toCellAssignment, Collectors.toList())
                )
        ));
    }
}
