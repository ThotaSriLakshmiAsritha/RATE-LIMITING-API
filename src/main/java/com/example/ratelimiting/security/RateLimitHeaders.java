package com.example.ratelimiting.security;

public final class RateLimitHeaders {
    private RateLimitHeaders() {
    }

    public static final String X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String X_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String RETRY_AFTER = "Retry-After";
}

