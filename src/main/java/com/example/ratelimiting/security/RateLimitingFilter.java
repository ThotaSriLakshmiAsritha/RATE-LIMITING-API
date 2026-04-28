package com.example.ratelimiting.security;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.observability.CorrelationIdFilter;
import com.example.ratelimiting.observability.RateLimitObservation;
import com.example.ratelimiting.ratelimit.RateLimiterService;
import com.example.ratelimiting.ratelimit.TokenBucketResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitingProperties properties;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            RateLimitingProperties properties,
            RateLimiterService rateLimiterService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
        protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = Optional.ofNullable(request.getRequestURI()).orElse("");
        return !properties.isEnabled() || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimiterService.RateLimiterDecision decision = rateLimiterService.evaluate(request);

        if (decision.headerResult() != null) {
            addHeaders(response, decision.headerResult());
        }

        if (decision.rejected()) {
            log.warn("request_observed method={} path={} endpoint={} status={} decision={} scope={} backend={} fallback={} duration_ms={} correlation_id={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    decision.endpointPattern(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    decision.decisionType().name(),
                    decision.failedScope(),
                    decision.backendType() != null ? decision.backendType().name() : "NONE",
                    decision.fallbackApplied(),
                    requestDurationMillis(request),
                    response.getHeader(CorrelationIdFilter.HEADER));
            writeRateLimitedResponse(request, response, decision.headerResult(), decision.message());
            return;
        }

        if (!decision.allowed()) {
            log.warn("request_observed method={} path={} endpoint={} status={} decision={} scope={} backend={} fallback={} duration_ms={} correlation_id={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    decision.endpointPattern(),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    decision.decisionType().name(),
                    Optional.ofNullable((String) request.getAttribute(RateLimitObservation.RATE_LIMIT_SCOPE)).orElse("NONE"),
                    decision.backendType() != null ? decision.backendType().name() : "NONE",
                    decision.fallbackApplied(),
                    requestDurationMillis(request),
                    response.getHeader(CorrelationIdFilter.HEADER));
            writeUnavailableResponse(response, decision.message());
            return;
        }

        if (decision.backendFailure()) {
            log.info("rate_limit_fallback_applied method={} path={} endpoint={} backend={} fallback={} correlation_id={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    decision.endpointPattern(),
                    decision.backendType(),
                    decision.fallbackApplied(),
                    response.getHeader(CorrelationIdFilter.HEADER));
        }
        filterChain.doFilter(request, response);
    }

    private static void addHeaders(HttpServletResponse response, TokenBucketResult result) {
        response.setHeader(RateLimitHeaders.X_RATE_LIMIT_LIMIT, Long.toString((long) result.limitCapacity()));
        response.setHeader(RateLimitHeaders.X_RATE_LIMIT_REMAINING, Long.toString(Math.max(0L, (long) Math.floor(result.remainingTokens()))));
        response.setHeader(RateLimitHeaders.X_RATE_LIMIT_RESET, Long.toString(result.resetUnixSeconds()));
        if (!result.allowed() && result.retryAfterSeconds() > 0) {
            response.setHeader(RateLimitHeaders.RETRY_AFTER, Long.toString(result.retryAfterSeconds()));
        }
    }

    private void writeRateLimitedResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            TokenBucketResult result,
            String message
        ) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "rate_limit_exceeded");
                body.put("message", message);
                body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
                body.put("limit", (long) result.limitCapacity());
                body.put("remaining", Math.max(0L, (long) Math.floor(result.remainingTokens())));
                body.put("reset", result.resetUnixSeconds());
                body.put("path", request.getRequestURI());

                try {
                        objectMapper.writeValue(response.getOutputStream(), body);
                } catch (IOException e) {
                        log.error("Failed to write JSON rate-limit response for path={}", request.getRequestURI(), e);
                        try {
                                response.resetBuffer();
                                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                                response.setContentType("text/plain");
                                response.getWriter().write("rate_limit_exceeded");
                                response.getWriter().flush();
                        } catch (IOException ignored) {
                                // Best effort fallback: status/header already set above.
                        }
                }
    }

    private void writeUnavailableResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", "rate_limiter_unavailable",
                "message", message,
                "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }

    private static String requestDurationMillis(HttpServletRequest request) {
        Object startedAt = request.getAttribute(RateLimitObservation.REQUEST_START_NANOS);
        if (!(startedAt instanceof Long started)) {
            return "0.000";
        }
        double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;
        return String.format(java.util.Locale.ROOT, "%.3f", elapsedMs);
    }
}

