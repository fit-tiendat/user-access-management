package com.r2s.core.dto;

import com.r2s.core.utils.CommonConstants;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private String status;     // SUCCESS | FAILED
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(CommonConstants.SUCCESS)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> failed(String message) {
        return ApiResponse.<T>builder()
                .status(CommonConstants.FAILED)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
