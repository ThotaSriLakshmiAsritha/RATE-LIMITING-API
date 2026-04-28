package com.example.ratelimiting.security;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitingFilterConfig {
    @Bean
    RateLimitingFilter rateLimitingFilter(
            RateLimitingProperties properties,
            RateLimiterService rateLimiterService,
            ObjectMapper objectMapper
    ) {
        return new RateLimitingFilter(properties, rateLimiterService, objectMapper);
    }
}

