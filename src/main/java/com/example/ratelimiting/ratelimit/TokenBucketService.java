package com.example.ratelimiting.ratelimit;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketService {
    private final StringRedisTemplate redisTemplate;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> tokenBucketScript;
    private final CircuitBreaker circuitBreaker;
    private final Counter redisFailures;

    public TokenBucketService(
            StringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setResultType(List.class);
        this.tokenBucketScript.setScriptText(loadScript("ratelimit/token_bucket.lua"));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisRateLimit");
        this.redisFailures = Counter.builder("rate_limit_redis_failures_total").register(meterRegistry);
    }

    public TokenBucketResult consume(TokenBucketRequest request) {
        Supplier<List<Object>> call = () -> {
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    tokenBucketScript,
                    List.of(request.redisKey()),
                    Double.toString(request.capacity()),
                    Double.toString(request.refillRatePerSecond()),
                    Double.toString(request.tokensRequired())
            );
            return result;
        };

        List<Object> result;
        try {
            result = CircuitBreaker.decorateSupplier(circuitBreaker, call).get();
        } catch (Exception e) {
            redisFailures.increment();
            throw new RateLimitBackendUnavailableException("Redis rate limit backend unavailable", e);
        }

        if (result == null || result.size() < 5) {
            return new TokenBucketResult(false, 0, request.capacity(), 0, 0);
        }

        boolean allowed = asLong(result.get(0)) == 1L;
        double remaining = asDouble(result.get(1));
        double limit = asDouble(result.get(2));
        long reset = asLong(result.get(3));
        long retryAfter = asLong(result.get(4));
        return new TokenBucketResult(allowed, remaining, limit, reset, retryAfter);
    }

    private static String loadScript(String classpathPath) {
        Resource resource = new ClassPathResource(classpathPath);
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Lua script: " + classpathPath, e);
        }
    }

    private static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static double asDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}

