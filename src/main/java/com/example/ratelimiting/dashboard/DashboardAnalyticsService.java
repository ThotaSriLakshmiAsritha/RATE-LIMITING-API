package com.example.ratelimiting.dashboard;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

@Service
public class DashboardAnalyticsService {
    private final MeterRegistry meterRegistry;

    public DashboardAnalyticsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public DashboardAnalyticsDtos.AnalyticsResponse currentSnapshot() {
        double allowed = sumCounter("rate_limiter_requests_total", "decision", "ALLOWED");
        double denied = sumCounter("rate_limiter_requests_total", "decision", "REJECTED");
        double unavailable = sumCounter("rate_limiter_requests_total", "decision", "UNAVAILABLE");
        double redisFailures = counterValue("rate_limit_redis_failures_total");

        DashboardAnalyticsDtos.RateLimitStats rateLimitStats = new DashboardAnalyticsDtos.RateLimitStats(
                allowed,
                denied,
                unavailable,
                redisFailures,
                averageTimerMillis("rate_limit_check_duration"),
                averageTimerMillis("rate_limit_redis_duration")
        );

        DashboardAnalyticsDtos.UsageMetrics usageMetrics = new DashboardAnalyticsDtos.UsageMetrics(
                timerCount("http_request_observation_duration"),
                averageTimerMillis("http_request_observation_duration"),
                topDecisionMetrics(),
                backendDecisionMetrics()
        );

        return new DashboardAnalyticsDtos.AnalyticsResponse(Instant.now(), rateLimitStats, usageMetrics);
    }

    private double sumCounter(String name, String tagKey, String tagValue) {
        return meterRegistry.find(name).tag(tagKey, tagValue).meters().stream()
                .mapToDouble(this::meterCount)
                .sum();
    }

    private double counterValue(String name) {
        return meterRegistry.find(name).meters().stream()
                .mapToDouble(this::meterCount)
                .sum();
    }

    private double timerCount(String name) {
        return meterRegistry.find(name).timers().stream()
                .mapToDouble(Timer::count)
                .sum();
    }

    private double averageTimerMillis(String name) {
        var timers = meterRegistry.find(name).timers();
        double totalCount = timers.stream().mapToDouble(Timer::count).sum();
        double totalMillis = timers.stream().mapToDouble(timer -> timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).sum();
        return totalCount == 0 ? 0.0 : totalMillis / totalCount;
    }

    private List<DashboardAnalyticsDtos.EndpointMetric> topDecisionMetrics() {
        return meterRegistry.find("rate_limit_checks_total").meters().stream()
                .collect(Collectors.groupingBy(
                        meter -> tagValue(meter, "endpoint", "unknown"),
                        Collectors.summingDouble(this::meterCount)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new DashboardAnalyticsDtos.EndpointMetric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Double> backendDecisionMetrics() {
        return meterRegistry.find("rate_limiter_requests_total").meters().stream()
                .collect(Collectors.groupingBy(
                        meter -> tagValue(meter, "backend", "NONE"),
                        Collectors.summingDouble(this::meterCount)
                ));
    }

    private double meterCount(Meter meter) {
        return StreamSupport.stream(meter.measure().spliterator(), false)
                .filter(measurement -> measurement.getStatistic() == Statistic.COUNT)
                .mapToDouble(Measurement::getValue)
                .sum();
    }

    private static String tagValue(Meter meter, String tagKey, String fallback) {
        String value = meter.getId().getTag(tagKey);
        return value != null ? value : fallback;
    }
}
