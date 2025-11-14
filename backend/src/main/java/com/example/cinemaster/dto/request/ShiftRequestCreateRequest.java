package com.example.cinemaster.dto.request;


import lombok.*;
import java.time.LocalDate;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftRequestCreateRequest {


    private Integer branchId;
    private String note;


    private List<ShiftItem> shifts;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftItem {
        private LocalDate date;
        private String shiftType; // MORNING / AFTERNOON / NIGHT
    }
}



