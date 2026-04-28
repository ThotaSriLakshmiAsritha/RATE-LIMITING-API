package com.example.ratelimiting.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DashboardAnalyticsDtos {
    private DashboardAnalyticsDtos() {
    }

    public record AnalyticsResponse(
            Instant generatedAt,
            RateLimitStats rateLimitStats,
            UsageMetrics usageMetrics
    ) {
    }

    public record RateLimitStats(
            double allowedRequests,
            double deniedRequests,
            double unavailableRequests,
            double redisFailures,
            double averageDecisionLatencyMs,
            double averageRedisTimeMs
    ) {
    }

    public record UsageMetrics(
            double requestObservations,
            double averageRequestLatencyMs,
            List<EndpointMetric> topDecisions,
            Map<String, Double> backendDecisions
    ) {
    }

    public record EndpointMetric(
            String tagValue,
            double count
    ) {
    }
}
