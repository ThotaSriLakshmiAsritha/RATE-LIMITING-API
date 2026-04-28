package com.example.ratelimiting.policy;

public record ResolvedPolicySnapshots(
        PolicySnapshot global,
        PolicySnapshot tenant,
        PolicySnapshot user,
        PolicySnapshot endpoint
) {
}
