package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.policy.PolicyScope;

public record ResolvedRateLimit(
        PolicyScope scope,
        RateLimitPolicy policy
) {
}
