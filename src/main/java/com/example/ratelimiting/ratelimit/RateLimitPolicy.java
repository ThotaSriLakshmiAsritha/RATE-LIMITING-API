package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;

public record RateLimitPolicy(
        int requestsPerMinute,
        int burstCapacity,
        RateLimitingProperties.Dimension dimension,
        String errorMessage,
        boolean bypass
) {
    public double refillRatePerSecond() {
        return requestsPerMinute / 60.0;
    }
}

