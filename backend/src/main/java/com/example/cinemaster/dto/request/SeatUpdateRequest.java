package com.example.cinemaster.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatUpdateRequest {

    Integer auditoriumID;
    Integer typeID;
    String seatNumber;
    String seatRow;
    Integer columnNumber;
    String status;
}
