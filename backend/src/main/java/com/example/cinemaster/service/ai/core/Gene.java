package com.example.cinemaster.service.ai.core;


import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gene {
    private int staffId;        // nhân viên
    private String shiftType;   // MORNING / AFTERNOON / NIGHT
    private String date;        // yyyy-MM-dd
    private double fitnessBonus; // điểm RL
}

