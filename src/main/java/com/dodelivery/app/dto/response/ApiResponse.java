package com.dodelivery.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Generic API envelope for success and error responses.
 * Error fields (error, message) are omitted on success responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int status,
        T data,
        String error,
        String message,
        Instant timestamp,
        String path
) {
    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(status, data, null, null, Instant.now(), null);
    }

    public static ApiResponse<Void> error(int status, String error, String message, String path) {
        return new ApiResponse<>(status, null, error, message, Instant.now(), path);
    }
}
