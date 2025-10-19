package com.example.cinemaster.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse <T> {
    private int code = 1000;
    private String message;
    private T result;

    // ✅ Thêm vào cuối file ApiResponse.java
    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder()
                .code(1000)
                .message("Success")
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(9999)
                .message(message)
                .build();
    }

}
