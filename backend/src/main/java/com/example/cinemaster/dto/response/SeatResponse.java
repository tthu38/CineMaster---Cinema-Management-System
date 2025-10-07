package com.example.cinemaster.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatResponse {
    Integer seatID;
    String seatNumber;
    String seatRow;
    Integer columnNumber;
    String status;

    // Thông tin từ Auditorium Entity
    Integer auditoriumID;
    String auditoriumName;

    // Thông tin từ SeatType Entity
    Integer typeID;
    String typeName;
}
