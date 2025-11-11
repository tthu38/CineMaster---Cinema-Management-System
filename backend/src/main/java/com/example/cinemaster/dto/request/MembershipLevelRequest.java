package com.example.cinemaster.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MembershipLevelRequest {

    @NotBlank
    @Size(max = 20)
    private String levelName;

    @NotNull @Min(0)
    private Integer minPoints;

    @NotNull @Min(0)
    private Integer maxPoints;

    private String benefits;
}
