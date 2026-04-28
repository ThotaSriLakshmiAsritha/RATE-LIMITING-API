package com.example.ratelimiting.security;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
import com.example.ratelimiting.ratelimit.RateLimitPolicyResolver;
import com.example.ratelimiting.ratelimit.TokenBucketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitingFilterConfig {
    @Bean
    RateLimitingFilter rateLimitingFilter(
            RateLimitingProperties properties,
            TokenBucketService tokenBucketService,
            RateLimitPolicyResolver policyResolver,
            RateLimitKeyFactory keyFactory,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        return new RateLimitingFilter(properties, tokenBucketService, policyResolver, keyFactory, meterRegistry, objectMapper);
    }
}

