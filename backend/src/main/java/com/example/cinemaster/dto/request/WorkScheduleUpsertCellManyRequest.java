// src/main/java/com/example/cinemaster/dto/request/WorkScheduleUpsertCellManyRequest.java
package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WorkScheduleUpsertCellManyRequest {
    @NotNull private Integer branchId;
    @NotNull private LocalDate date;
    @NotBlank private String shiftType; // MORNING / AFTERNOON / NIGHT
    @NotNull @Size(min = 0) private List<Integer> accountIds;
    private LocalTime startTime;
    private LocalTime endTime;
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    public List<Integer> getAccountIds() { return accountIds; }
    public void setAccountIds(List<Integer> accountIds) { this.accountIds = accountIds; }
}
