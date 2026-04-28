package com.example.ratelimiting.security;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
import com.example.ratelimiting.ratelimit.RateLimitPolicy;
import com.example.ratelimiting.ratelimit.RateLimitPolicyResolver;
import com.example.ratelimiting.ratelimit.RateLimitBackendUnavailableException;
import com.example.ratelimiting.ratelimit.TokenBucketRequest;
import com.example.ratelimiting.ratelimit.TokenBucketResult;
import com.example.ratelimiting.ratelimit.TokenBucketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final TokenBucketService tokenBucketService;
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitKeyFactory keyFactory;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            RateLimitingProperties properties,
            TokenBucketService tokenBucketService,
            RateLimitPolicyResolver policyResolver,
            RateLimitKeyFactory keyFactory,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.tokenBucketService = tokenBucketService;
        this.policyResolver = policyResolver;
        this.keyFactory = keyFactory;
        this.meterRegistry = meterRegistry;
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
        var resolved = policyResolver.resolve(request);
        RateLimitPolicy policy = resolved.policy();
        if (policy.bypass()) {
            filterChain.doFilter(request, response);
            return;
        }

        var key = keyFactory.createKey(policy.dimension(), resolved.endpointPattern(), request);
        TokenBucketResult result;
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            result = tokenBucketService.consume(new TokenBucketRequest(
                            key.toRedisKey(),
                            policy.burstCapacity(),
                            policy.refillRatePerSecond(),
                            1.0
                    )
            );
            sample.stop(meterRegistry.timer(
                    "rate_limit_check_duration",
                    "endpoint", resolved.endpointPattern(),
                    "dimension", policy.dimension().name()
            ));
        } catch (RateLimitBackendUnavailableException e) {
            meterRegistry.counter("rate_limit_checks_total",
                    "endpoint", resolved.endpointPattern(),
                    "dimension", policy.dimension().name(),
                    "result", "backend_unavailable"
            ).increment();
            if (properties.getFallbackMode() == RateLimitingProperties.FallbackMode.ALLOW || isPingPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "error", "rate_limiter_unavailable",
                    "message", "Rate limiting unavailable. Please try again later.",
                    "status", HttpStatus.SERVICE_UNAVAILABLE.value()
            ));
            return;
        }

        addHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit_checks_total",
                    "endpoint", resolved.endpointPattern(),
                    "dimension", policy.dimension().name(),
                    "result", "rejected"
            ).increment();

            String msg = (policy.errorMessage() == null || policy.errorMessage().isBlank())
                    ? "Rate limit exceeded. Please try again later."
                    : policy.errorMessage();
            log.warn("Rate limit exceeded endpoint={} dimension={} remainingTokens={}",
                    resolved.endpointPattern(), policy.dimension().name(), result.remainingTokens());
            writeRateLimitedResponse(request, response, result, msg);
            return;
        }

        meterRegistry.counter("rate_limit_checks_total",
                "endpoint", resolved.endpointPattern(),
                "dimension", policy.dimension().name(),
                "result", "allowed"
        ).increment();
        log.debug("Rate limit allowed endpoint={} dimension={} remainingTokens={}",
                resolved.endpointPattern(), policy.dimension().name(), result.remainingTokens());
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

        private static boolean isPingPath(HttpServletRequest request) {
                String path = Optional.ofNullable(request.getRequestURI()).orElse("");
                return "/ping".equals(path);
        }

    // identity extraction moved to ClientIdentityExtractor
}

