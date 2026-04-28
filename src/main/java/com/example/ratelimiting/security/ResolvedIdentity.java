package com.example.ratelimiting.security;

public record ResolvedIdentity(
        IdentityType type,
        String subject,
        String userId,
        String apiKey,
        String clientIp
) {
}
