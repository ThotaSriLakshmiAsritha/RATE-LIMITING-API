package com.example.ratelimiting.policy;

public record PolicyContext(
        String tenantId,
        String userId,
        String endpointPattern
) {
}
