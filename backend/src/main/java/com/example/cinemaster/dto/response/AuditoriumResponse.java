// src/main/java/com/example/cinemaster/dto/response/AuditoriumLite.java
package com.example.cinemaster.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class AuditoriumResponse {
    private Integer id;
    private String name;
}
