package com.example.ratelimiting.observability;

public final class RateLimitObservation {
    private RateLimitObservation() {
    }

    public static final String REQUEST_START_NANOS = "observability.requestStartNanos";
    public static final String RATE_LIMIT_DECISION = "observability.rateLimit.decision";
    public static final String RATE_LIMIT_ENDPOINT = "observability.rateLimit.endpoint";
    public static final String RATE_LIMIT_SCOPE = "observability.rateLimit.scope";
    public static final String RATE_LIMIT_BACKEND = "observability.rateLimit.backend";
    public static final String RATE_LIMIT_FALLBACK = "observability.rateLimit.fallback";
}
