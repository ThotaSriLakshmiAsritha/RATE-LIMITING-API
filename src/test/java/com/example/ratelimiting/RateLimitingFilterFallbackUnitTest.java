package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.RateLimitBackendUnavailableException;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
import com.example.ratelimiting.ratelimit.RateLimitPolicy;
import com.example.ratelimiting.ratelimit.RateLimitPolicyResolver;
import com.example.ratelimiting.ratelimit.TokenBucketService;
import com.example.ratelimiting.security.RateLimitingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterFallbackUnitTest {

    @Test
    void failOpenAllowsRequestWhenRedisUnavailable() throws Exception {
        RateLimitingProperties props = new RateLimitingProperties();
        props.setFallbackMode(RateLimitingProperties.FallbackMode.ALLOW);

        TokenBucketService tokenBucketService = Mockito.mock(TokenBucketService.class);
        Mockito.when(tokenBucketService.consume(Mockito.any()))
                .thenThrow(new RateLimitBackendUnavailableException("down", new RuntimeException("boom")));

        RateLimitPolicyResolver resolver = Mockito.mock(RateLimitPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any())).thenReturn(
                new RateLimitPolicyResolver.ResolvedPolicy(
                        new RateLimitPolicy(100, 20, RateLimitingProperties.Dimension.IP, "", false),
                        "/x"
                )
        );

        RateLimitKeyFactory keyFactory = Mockito.mock(RateLimitKeyFactory.class);
        Mockito.when(keyFactory.createKey(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new com.example.ratelimiting.ratelimit.RateLimitKey(RateLimitingProperties.Dimension.IP, "1.2.3.4", "hash"));

        RateLimitingFilter filter = new RateLimitingFilter(props, tokenBucketService, resolver, keyFactory, new SimpleMeterRegistry(), objectMapper());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, resp, chain);
        Mockito.verify(chain).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void failClosedReturns503WhenRedisUnavailable() throws Exception {
        RateLimitingProperties props = new RateLimitingProperties();
        props.setFallbackMode(RateLimitingProperties.FallbackMode.DENY);

        TokenBucketService tokenBucketService = Mockito.mock(TokenBucketService.class);
        Mockito.when(tokenBucketService.consume(Mockito.any()))
                .thenThrow(new RateLimitBackendUnavailableException("down", new RuntimeException("boom")));

        RateLimitPolicyResolver resolver = Mockito.mock(RateLimitPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any())).thenReturn(
                new RateLimitPolicyResolver.ResolvedPolicy(
                        new RateLimitPolicy(100, 20, RateLimitingProperties.Dimension.IP, "", false),
                        "/x"
                )
        );

        RateLimitKeyFactory keyFactory = Mockito.mock(RateLimitKeyFactory.class);
        Mockito.when(keyFactory.createKey(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new com.example.ratelimiting.ratelimit.RateLimitKey(RateLimitingProperties.Dimension.IP, "1.2.3.4", "hash"));

        RateLimitingFilter filter = new RateLimitingFilter(props, tokenBucketService, resolver, keyFactory, new SimpleMeterRegistry(), objectMapper());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, resp, chain);
        org.assertj.core.api.Assertions.assertThat(resp.getStatus()).isEqualTo(503);
        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}

