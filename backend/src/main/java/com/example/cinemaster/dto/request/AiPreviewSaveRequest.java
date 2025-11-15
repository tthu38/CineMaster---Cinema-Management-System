package com.example.cinemaster.dto.request;


import lombok.Data;
import java.util.Map;
import java.util.List;


@Data
public class AiPreviewSaveRequest {
    private Map<String, Map<String, List<Integer>>> matrix;
}

