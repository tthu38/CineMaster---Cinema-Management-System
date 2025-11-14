package com.example.cinemaster.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OccupiedSeatResponse {
    private Integer seatId;
    private String status; // "BOOKED" hoặc "HOLDING"
}
