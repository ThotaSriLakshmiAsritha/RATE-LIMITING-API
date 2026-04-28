package com.example.ratelimiting.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private final Timer requestTimer;

    public RequestLoggingInterceptor(MeterRegistry meterRegistry) {
        this.requestTimer = Timer.builder("http_request_observation_duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (request.getAttribute(RateLimitObservation.REQUEST_START_NANOS) == null) {
            request.setAttribute(RateLimitObservation.REQUEST_START_NANOS, System.nanoTime());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex
    ) {
        long startedAt = Optional.ofNullable((Long) request.getAttribute(RateLimitObservation.REQUEST_START_NANOS))
                .orElse(System.nanoTime());
        long elapsedNanos = System.nanoTime() - startedAt;
        requestTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);

        String decision = attribute(request, RateLimitObservation.RATE_LIMIT_DECISION, "PASSED");
        String endpoint = attribute(request, RateLimitObservation.RATE_LIMIT_ENDPOINT, request.getRequestURI());
        String scope = attribute(request, RateLimitObservation.RATE_LIMIT_SCOPE, "NONE");
        String backend = attribute(request, RateLimitObservation.RATE_LIMIT_BACKEND, "NONE");
        String fallback = attribute(request, RateLimitObservation.RATE_LIMIT_FALLBACK, "false");
        String correlationId = Optional.ofNullable(response.getHeader(CorrelationIdFilter.HEADER)).orElse("missing");

        log.info(
                "request_observed method={} path={} endpoint={} status={} decision={} scope={} backend={} fallback={} duration_ms={} correlation_id={}",
                request.getMethod(),
                request.getRequestURI(),
                endpoint,
                response.getStatus(),
                decision,
                scope,
                backend,
                fallback,
                String.format(java.util.Locale.ROOT, "%.3f", elapsedNanos / 1_000_000.0),
                correlationId
        );
        if (ex != null) {
            log.error("request_failed method={} path={} status={} correlation_id={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), correlationId, ex);
        }
    }

    private static String attribute(HttpServletRequest request, String key, String fallback) {
        Object value = request.getAttribute(key);
        return value == null ? fallback : value.toString();
    }
}
