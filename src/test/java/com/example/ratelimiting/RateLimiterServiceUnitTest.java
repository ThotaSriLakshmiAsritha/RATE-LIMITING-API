package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.observability.RateLimitObservation;
import com.example.ratelimiting.policy.PolicyScope;
import com.example.ratelimiting.ratelimit.BackendType;
import com.example.ratelimiting.ratelimit.RateLimitBackendUnavailableException;
import com.example.ratelimiting.ratelimit.RateLimitKey;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
import com.example.ratelimiting.ratelimit.RateLimitPolicy;
import com.example.ratelimiting.ratelimit.RateLimitPolicyResolver;
import com.example.ratelimiting.ratelimit.RateLimiterService;
import com.example.ratelimiting.ratelimit.RequestCostResolver;
import com.example.ratelimiting.ratelimit.ResolvedRateLimit;
import com.example.ratelimiting.ratelimit.TokenBucketResult;
import com.example.ratelimiting.ratelimit.TokenBucketService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimiterServiceUnitTest {

    @Test
    void dbFailureFallsBackToNonDbPoliciesWhenConfiguredFailOpen() {
        RateLimitingProperties props = new RateLimitingProperties();
        props.getFailureHandling().setDbFallbackMode(RateLimitingProperties.FallbackMode.ALLOW);

        TokenBucketService tokenBucketService = Mockito.mock(TokenBucketService.class);
        Mockito.when(tokenBucketService.consumeBatch(Mockito.anyList()))
                .thenReturn(List.of(new TokenBucketResult(true, 10, 20, 123, 0)));

        RateLimitPolicyResolver resolver = Mockito.mock(RateLimitPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any()))
                .thenThrow(new RateLimitBackendUnavailableException("db down", BackendType.DATABASE, true, new RuntimeException("boom")));
        Mockito.when(resolver.resolveWithoutDb(Mockito.any()))
                .thenReturn(resolvedPolicy());

        RateLimitKeyFactory keyFactory = Mockito.mock(RateLimitKeyFactory.class);
        Mockito.when(keyFactory.createKey(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new RateLimitKey(RateLimitingProperties.Dimension.IP, "1.2.3.4", "hash", 0));
        Mockito.when(keyFactory.effectiveShardCount(Mockito.any())).thenReturn(1);

        RateLimiterService service = new RateLimiterService(
                props, tokenBucketService, resolver, keyFactory, new RequestCostResolver(props), new SimpleMeterRegistry()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        RateLimiterService.RateLimiterDecision decision = service.evaluate(request);

        org.assertj.core.api.Assertions.assertThat(decision.allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(decision.fallbackApplied()).isTrue();
        org.assertj.core.api.Assertions.assertThat(request.getAttribute(RateLimitObservation.RATE_LIMIT_DECISION)).isEqualTo("ALLOWED");
        Mockito.verify(resolver).resolveWithoutDb(Mockito.any());
    }

    @Test
    void redisFailureReturnsUnavailableWhenFailClosed() {
        RateLimitingProperties props = new RateLimitingProperties();
        props.getFailureHandling().setRedisFallbackMode(RateLimitingProperties.FallbackMode.DENY);

        TokenBucketService tokenBucketService = Mockito.mock(TokenBucketService.class);
        Mockito.when(tokenBucketService.consumeBatch(Mockito.anyList()))
                .thenThrow(new RateLimitBackendUnavailableException("redis down", BackendType.REDIS, true, new RuntimeException("boom")));

        RateLimitPolicyResolver resolver = Mockito.mock(RateLimitPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any())).thenReturn(resolvedPolicy());

        RateLimitKeyFactory keyFactory = Mockito.mock(RateLimitKeyFactory.class);
        Mockito.when(keyFactory.createKey(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new RateLimitKey(RateLimitingProperties.Dimension.IP, "1.2.3.4", "hash", 0));
        Mockito.when(keyFactory.effectiveShardCount(Mockito.any())).thenReturn(1);

        RateLimiterService service = new RateLimiterService(
                props, tokenBucketService, resolver, keyFactory, new RequestCostResolver(props), new SimpleMeterRegistry()
        );

        RateLimiterService.RateLimiterDecision decision = service.evaluate(new MockHttpServletRequest("GET", "/x"));

        org.assertj.core.api.Assertions.assertThat(decision.decisionType()).isEqualTo(RateLimiterService.DecisionType.UNAVAILABLE);
        org.assertj.core.api.Assertions.assertThat(decision.backendType()).isEqualTo(BackendType.REDIS);
    }

    @Test
    void unexpectedFailureDoesNotCrashAndReturnsUnavailable() {
        RateLimitingProperties props = new RateLimitingProperties();

        TokenBucketService tokenBucketService = Mockito.mock(TokenBucketService.class);
        RateLimitPolicyResolver resolver = Mockito.mock(RateLimitPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any())).thenThrow(new IllegalStateException("bad state"));
        RateLimitKeyFactory keyFactory = Mockito.mock(RateLimitKeyFactory.class);

        RateLimiterService service = new RateLimiterService(
                props, tokenBucketService, resolver, keyFactory, new RequestCostResolver(props), new SimpleMeterRegistry()
        );

        RateLimiterService.RateLimiterDecision decision = service.evaluate(new MockHttpServletRequest("GET", "/x"));

        org.assertj.core.api.Assertions.assertThat(decision.decisionType()).isEqualTo(RateLimiterService.DecisionType.UNAVAILABLE);
        org.assertj.core.api.Assertions.assertThat(decision.backendType()).isEqualTo(BackendType.UNEXPECTED);
    }

    private static RateLimitPolicyResolver.ResolvedPolicy resolvedPolicy() {
        return new RateLimitPolicyResolver.ResolvedPolicy(
                List.of(new ResolvedRateLimit(
                        PolicyScope.ENDPOINT,
                        new RateLimitPolicy(100, 20, RateLimitingProperties.Dimension.IP, "", false)
                )),
                "/x",
                false
        );
    }
}
