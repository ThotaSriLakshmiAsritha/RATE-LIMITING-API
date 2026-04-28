package com.example.ratelimiting.policy;

import com.example.ratelimiting.config.RateLimitingProperties;
import java.util.UUID;

public record PolicySnapshot(
        UUID id,
        String tenantId,
        PolicyScope scopeType,
        String scopeId,
        String endpointPattern,
        Integer requestsPerMinute,
        Integer burstCapacity,
        RateLimitingProperties.Dimension dimension,
        String errorMessage,
        PolicyMode mode,
        Integer priority,
        long version
) {
}
