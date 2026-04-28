package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.BackendType;
import com.example.ratelimiting.ratelimit.RateLimiterService;
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

        RateLimiterService service = Mockito.mock(RateLimiterService.class);
        Mockito.when(service.evaluate(Mockito.any())).thenReturn(
                RateLimiterService.RateLimiterDecision.allowed(
                        "/x",
                        null,
                        true,
                        true,
                        BackendType.REDIS,
                        "fallback"
                )
        );

        RateLimitingFilter filter = new RateLimitingFilter(props, service, objectMapper());

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

        RateLimiterService service = Mockito.mock(RateLimiterService.class);
        Mockito.when(service.evaluate(Mockito.any())).thenReturn(
                RateLimiterService.RateLimiterDecision.unavailable(
                        BackendType.REDIS,
                        "Rate limiting unavailable. Please try again later."
                )
        );

        RateLimitingFilter filter = new RateLimitingFilter(props, service, objectMapper());

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

