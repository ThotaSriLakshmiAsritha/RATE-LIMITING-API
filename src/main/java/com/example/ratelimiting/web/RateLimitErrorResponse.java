package com.example.ratelimiting.web;

import java.time.Instant;

public record RateLimitErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        RateLimitInfo rateLimit
) {
    public record RateLimitInfo(long limit, long remaining, long reset) {
    }
}

