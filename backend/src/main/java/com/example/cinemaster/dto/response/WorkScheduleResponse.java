package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleResponse {

    Integer id;
    Integer accountId;
    String accountName;
    Integer branchId;
    String branchName;
    LocalDate shiftDate;
    LocalTime startTime;
    LocalTime endTime;
    String shiftType;
    String note;
}
