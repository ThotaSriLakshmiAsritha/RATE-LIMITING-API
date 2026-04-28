package com.example.ratelimiting.ratelimit;

public record TokenBucketResult(
        boolean allowed,
        double remainingTokens,
        double limitCapacity,
        long resetUnixSeconds,
        long retryAfterSeconds
) {
}

