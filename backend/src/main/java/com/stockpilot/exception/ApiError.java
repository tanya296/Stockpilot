package com.stockpilot.exception;

import java.time.Instant;
import java.util.Map;

/** Every error the API returns has this same JSON shape, so the frontend can handle them uniformly. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of());
    }
}
