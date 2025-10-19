package com.example.cinemaster.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatResponse {
    Integer seatID;
    String seatNumber;
    String seatRow;
    Integer columnNumber;
    String status;

    // Auditorium
    Integer auditoriumID;
    String auditoriumName;

    // SeatType
    Integer typeID;
    String typeName;

    // Branch
    Integer branchID;
    String branchName;

    LocalDateTime lockedUntil;
}
