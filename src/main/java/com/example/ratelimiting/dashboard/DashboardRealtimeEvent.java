package com.example.ratelimiting.dashboard;

import java.time.Instant;

public record DashboardRealtimeEvent(
        String type,
        Instant emittedAt,
        Object payload
) {
}
