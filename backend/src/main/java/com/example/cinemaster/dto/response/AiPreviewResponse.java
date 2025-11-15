package com.example.cinemaster.dto.response;


import lombok.Data;
import java.util.List;
import java.util.Map;


@Data
public class AiPreviewResponse {
    private Map<String, Map<String, List<AiStaffResponse>>> matrix;
}

