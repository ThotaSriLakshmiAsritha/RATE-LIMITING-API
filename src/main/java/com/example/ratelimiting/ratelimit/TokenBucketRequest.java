package com.example.ratelimiting.ratelimit;

public record TokenBucketRequest(
        String redisKey,
        double capacity,
        double refillRatePerSecond,
        double tokensRequired
) {
}

