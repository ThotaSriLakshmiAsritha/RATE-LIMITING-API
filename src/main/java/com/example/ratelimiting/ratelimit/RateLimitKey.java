package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;

public record RateLimitKey(
        RateLimitingProperties.Dimension dimension,
        String identifier,
        String endpointHash
) {
    public String toRedisKey() {
        return "rate_limit:" + dimension.name() + ":" + identifier + ":" + endpointHash;
    }
}

