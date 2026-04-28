package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.observability.RateLimitObservation;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitingProperties properties;
    private final TokenBucketService tokenBucketService;
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitKeyFactory keyFactory;
    private final RequestCostResolver requestCostResolver;
    private final MeterRegistry meterRegistry;

    public RateLimiterService(
            RateLimitingProperties properties,
            TokenBucketService tokenBucketService,
            RateLimitPolicyResolver policyResolver,
            RateLimitKeyFactory keyFactory,
            RequestCostResolver requestCostResolver,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.tokenBucketService = tokenBucketService;
        this.policyResolver = policyResolver;
        this.keyFactory = keyFactory;
        this.requestCostResolver = requestCostResolver;
        this.meterRegistry = meterRegistry;
    }

    public RateLimiterDecision evaluate(HttpServletRequest request) {
        try {
            RateLimitPolicyResolver.ResolvedPolicy resolved = policyResolver.resolve(request);
            RateLimiterDecision decision = evaluateResolved(request, resolved, false);
            applyObservation(request, decision);
            return decision;
        } catch (RateLimitBackendUnavailableException e) {
            RateLimiterDecision decision = handleBackendFailure(request, e);
            applyObservation(request, decision);
            return decision;
        } catch (RuntimeException e) {
            log.error("Unexpected rate limiting failure for path={}", request.getRequestURI(), e);
            RateLimiterDecision decision = buildUnexpectedFallback();
            applyObservation(request, decision);
            return decision;
        }
    }

    private RateLimiterDecision evaluateResolved(
            HttpServletRequest request,
            RateLimitPolicyResolver.ResolvedPolicy resolved,
            boolean fallback
    ) {
        if (resolved.bypass()) {
            return RateLimiterDecision.allowed(resolved.endpointPattern(), null, fallback, false, null, null);
        }

        double requestCost = requestCostResolver.resolveCost(request);
        double adaptiveScale = resolveAdaptiveScale();
        List<ResolvedRateLimit> policies = resolved.policies();
        List<TokenBucketRequest> bucketRequests = policies.stream()
                .map(entry -> toBucketRequest(entry, resolved.endpointPattern(), request, requestCost, adaptiveScale))
                .toList();

        Timer.Sample sample = Timer.start(meterRegistry);
        List<TokenBucketResult> results;
        try {
            results = tokenBucketService.consumeBatch(bucketRequests);
        } finally {
            sample.stop(meterRegistry.timer(
                    "rate_limit_check_duration",
                    "endpoint", resolved.endpointPattern(),
                    "dimension", "MULTI"
            ));
        }

        EvaluationOutcome outcome = evaluateOutcome(policies, results);
        if (!outcome.allowed()) {
            RateLimitPolicy failedPolicy = outcome.failedPolicy();
            meterRegistry.counter(
                    "rate_limit_checks_total",
                    "endpoint", resolved.endpointPattern(),
                    "dimension", failedPolicy.dimension().name(),
                    "result", "rejected"
            ).increment();
            String message = (failedPolicy.errorMessage() == null || failedPolicy.errorMessage().isBlank())
                    ? "Rate limit exceeded. Please try again later."
                    : failedPolicy.errorMessage();
            return RateLimiterDecision.rejected(
                    resolved.endpointPattern(),
                    outcome.headerResult(),
                    failedPolicy,
                    outcome.failedScope(),
                    message,
                    fallback
            );
        }

        meterRegistry.counter(
                "rate_limit_checks_total",
                "endpoint", resolved.endpointPattern(),
                "dimension", "MULTI",
                "result", "allowed"
        ).increment();
        return RateLimiterDecision.allowed(
                resolved.endpointPattern(),
                outcome.headerResult(),
                fallback,
                false,
                null,
                null
        );
    }

    private RateLimiterDecision handleBackendFailure(HttpServletRequest request, RateLimitBackendUnavailableException failure) {
        meterRegistry.counter(
                "rate_limit_checks_total",
                "endpoint", Optional.ofNullable(request.getRequestURI()).orElse("/"),
                "dimension", "MULTI",
                "result", "backend_unavailable",
                "backend", failure.backendType().name()
        ).increment();

        if (failure.backendType() == BackendType.DATABASE
                && fallbackModeFor(BackendType.DATABASE) == RateLimitingProperties.FallbackMode.ALLOW) {
            try {
                RateLimitPolicyResolver.ResolvedPolicy fallbackResolved = policyResolver.resolveWithoutDb(request);
                return evaluateResolved(request, fallbackResolved, true);
            } catch (RateLimitBackendUnavailableException nestedFailure) {
                return handleBackendFailureAfterDbFallback(request, nestedFailure);
            } catch (RuntimeException nestedUnexpected) {
                log.error("Fallback rate limiting after DB failure also failed for path={}", request.getRequestURI(), nestedUnexpected);
                return buildUnexpectedFallback();
            }
        }

        if (shouldAllowOnFailure(request, failure.backendType())) {
            return RateLimiterDecision.allowed(
                    Optional.ofNullable(request.getRequestURI()).orElse("/"),
                    null,
                    true,
                    true,
                    failure.backendType(),
                    "Rate limiting fallback allowed request"
            );
        }
        return RateLimiterDecision.unavailable(
                failure.backendType(),
                "Rate limiting unavailable. Please try again later."
        );
    }

    private RateLimiterDecision handleBackendFailureAfterDbFallback(
            HttpServletRequest request,
            RateLimitBackendUnavailableException failure
    ) {
        if (shouldAllowOnFailure(request, failure.backendType())) {
            return RateLimiterDecision.allowed(
                    Optional.ofNullable(request.getRequestURI()).orElse("/"),
                    null,
                    true,
                    true,
                    failure.backendType(),
                    "Rate limiting fallback allowed request"
            );
        }
        return RateLimiterDecision.unavailable(
                failure.backendType(),
                "Rate limiting unavailable. Please try again later."
        );
    }

    private RateLimiterDecision buildUnexpectedFallback() {
        if (properties.getFailureHandling().getUnexpectedFallbackMode() == RateLimitingProperties.FallbackMode.ALLOW) {
            return RateLimiterDecision.allowed("/", null, true, true, BackendType.UNEXPECTED, "Unexpected rate limiting failure");
        }
        return RateLimiterDecision.unavailable(BackendType.UNEXPECTED, "Rate limiting unavailable. Please try again later.");
    }

    private void applyObservation(HttpServletRequest request, RateLimiterDecision decision) {
        request.setAttribute(RateLimitObservation.RATE_LIMIT_DECISION, decision.decisionType().name());
        request.setAttribute(RateLimitObservation.RATE_LIMIT_ENDPOINT, decision.endpointPattern());
        request.setAttribute(RateLimitObservation.RATE_LIMIT_SCOPE, decision.failedScope());
        request.setAttribute(RateLimitObservation.RATE_LIMIT_FALLBACK, decision.fallbackApplied());
        request.setAttribute(
                RateLimitObservation.RATE_LIMIT_BACKEND,
                decision.backendType() != null ? decision.backendType().name() : "NONE"
        );
        meterRegistry.counter(
                "rate_limiter_requests_total",
                "decision", decision.decisionType().name(),
                "backend", decision.backendType() != null ? decision.backendType().name() : "NONE",
                "fallback", Boolean.toString(decision.fallbackApplied())
        ).increment();
    }

    private boolean shouldAllowOnFailure(HttpServletRequest request, BackendType backendType) {
        return isPingPath(request) || fallbackModeFor(backendType) == RateLimitingProperties.FallbackMode.ALLOW;
    }

    private RateLimitingProperties.FallbackMode fallbackModeFor(BackendType backendType) {
        RateLimitingProperties.FailureHandling failureHandling = properties.getFailureHandling();
        return switch (backendType) {
            case REDIS -> failureHandling.getRedisFallbackMode() != null
                    ? failureHandling.getRedisFallbackMode()
                    : properties.getFallbackMode();
            case DATABASE -> failureHandling.getDbFallbackMode() != null
                    ? failureHandling.getDbFallbackMode()
                    : properties.getFallbackMode();
            case UNEXPECTED -> failureHandling.getUnexpectedFallbackMode() != null
                    ? failureHandling.getUnexpectedFallbackMode()
                    : properties.getFallbackMode();
        };
    }

    private TokenBucketRequest toBucketRequest(
            ResolvedRateLimit resolvedRateLimit,
            String endpointPattern,
            HttpServletRequest request,
            double requestCost,
            double adaptiveScale
    ) {
        RateLimitPolicy policy = resolvedRateLimit.policy();
        RateLimitKey key = keyFactory.createKey(resolvedRateLimit.scope(), policy.dimension(), endpointPattern, request);
        int shardCount = keyFactory.effectiveShardCount(resolvedRateLimit.scope());
        double shardScale = 1.0 / shardCount;
        double capacity = Math.max(1.0, policy.burstCapacity() * shardScale * adaptiveScale);
        double refillRate = Math.max(1.0 / 60.0, policy.refillRatePerSecond() * shardScale * adaptiveScale);
        return new TokenBucketRequest(key.toRedisKey(), capacity, refillRate, requestCost);
    }

    private double resolveAdaptiveScale() {
        RateLimitingProperties.Adaptive adaptive = properties.getAdaptive();
        if (adaptive == null || !adaptive.isEnabled()) {
            return 1.0;
        }
        return tokenBucketService.adaptiveScale(adaptive.getMaxLatencyMs(), adaptive.getMinScale());
    }

    private static EvaluationOutcome evaluateOutcome(List<ResolvedRateLimit> policies, List<TokenBucketResult> results) {
        ResolvedRateLimit failedPolicy = null;
        TokenBucketResult failedResult = null;
        ResolvedRateLimit headerPolicy = null;
        TokenBucketResult headerResult = null;
        for (int i = 0; i < policies.size(); i++) {
            ResolvedRateLimit currentPolicy = policies.get(i);
            TokenBucketResult currentResult = results.get(i);
            if (headerResult == null || isMoreRestrictive(currentResult, headerResult)) {
                headerPolicy = currentPolicy;
                headerResult = currentResult;
            }
            if (!currentResult.allowed()) {
                failedPolicy = currentPolicy;
                failedResult = currentResult;
                break;
            }
        }
        if (failedPolicy != null) {
            return new EvaluationOutcome(false, failedPolicy.policy(), failedPolicy.scope().name(), failedResult, headerResult);
        }
        if (headerPolicy == null || headerResult == null) {
            throw new IllegalStateException("No rate limit evaluation results were produced");
        }
        return new EvaluationOutcome(true, headerPolicy.policy(), headerPolicy.scope().name(), headerResult, headerResult);
    }

    private static boolean isMoreRestrictive(TokenBucketResult candidate, TokenBucketResult current) {
        double candidateRatio = candidate.limitCapacity() <= 0 ? Double.NEGATIVE_INFINITY : candidate.remainingTokens() / candidate.limitCapacity();
        double currentRatio = current.limitCapacity() <= 0 ? Double.NEGATIVE_INFINITY : current.remainingTokens() / current.limitCapacity();
        return candidateRatio < currentRatio;
    }

    private static boolean isPingPath(HttpServletRequest request) {
        String path = Optional.ofNullable(request.getRequestURI()).orElse("");
        return "/ping".equals(path);
    }

    private record EvaluationOutcome(
            boolean allowed,
            RateLimitPolicy failedPolicy,
            String failedScope,
            TokenBucketResult failedResult,
            TokenBucketResult headerResult
    ) {
    }

    public record RateLimiterDecision(
            DecisionType decisionType,
            String endpointPattern,
            TokenBucketResult headerResult,
            RateLimitPolicy failedPolicy,
            String failedScope,
            String message,
            boolean fallbackApplied,
            boolean backendFailure,
            BackendType backendType
    ) {
        public static RateLimiterDecision allowed(
                String endpointPattern,
                TokenBucketResult headerResult,
                boolean fallbackApplied,
                boolean backendFailure,
                BackendType backendType,
                String message
        ) {
            return new RateLimiterDecision(
                    DecisionType.ALLOWED,
                    endpointPattern,
                    headerResult,
                    null,
                    null,
                    message,
                    fallbackApplied,
                    backendFailure,
                    backendType
            );
        }

        public static RateLimiterDecision rejected(
                String endpointPattern,
                TokenBucketResult headerResult,
                RateLimitPolicy failedPolicy,
                String failedScope,
                String message,
                boolean fallbackApplied
        ) {
            return new RateLimiterDecision(
                    DecisionType.REJECTED,
                    endpointPattern,
                    headerResult,
                    failedPolicy,
                    failedScope,
                    message,
                    fallbackApplied,
                    false,
                    null
            );
        }

        public static RateLimiterDecision unavailable(BackendType backendType, String message) {
            return new RateLimiterDecision(
                    DecisionType.UNAVAILABLE,
                    "/",
                    null,
                    null,
                    null,
                    message,
                    true,
                    true,
                    backendType
            );
        }

        public boolean allowed() {
            return decisionType == DecisionType.ALLOWED;
        }

        public boolean rejected() {
            return decisionType == DecisionType.REJECTED;
        }
    }

    public enum DecisionType {
        ALLOWED,
        REJECTED,
        UNAVAILABLE
    }
}
