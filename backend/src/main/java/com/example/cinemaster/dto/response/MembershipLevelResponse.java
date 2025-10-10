package com.example.cinemaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Builder @AllArgsConstructor
public class MembershipLevelResponse {
    private Integer id;
    private String levelName;
    private Integer minPoints;
    private Integer maxPoints;
    private String benefits;
}
