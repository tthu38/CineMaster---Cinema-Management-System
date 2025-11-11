package com.example.cinemaster.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditoriumResponse {
    Integer auditoriumID;
    String name;
    Integer capacity;
    String type;
    Integer branchId;
    String branchName;
    Boolean isActive;
}
