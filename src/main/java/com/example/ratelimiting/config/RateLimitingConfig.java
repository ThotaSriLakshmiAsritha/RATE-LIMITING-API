package com.example.ratelimiting.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RateLimitingProperties.class})
public class RateLimitingConfig {
}

