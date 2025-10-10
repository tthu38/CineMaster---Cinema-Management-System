package com.example.cinemaster.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffSimpleResponse {
    private Integer id;
    private String fullName;
}
