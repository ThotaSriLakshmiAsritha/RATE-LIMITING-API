package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;
    private final CircuitBreaker circuitBreaker;
    private final Counter redisFailures;
    private final Timer redisTimer;
    private final AtomicReference<Double> observedLatencyMs = new AtomicReference<>(0.0);
    private final RateLimitingProperties properties;

    public TokenBucketService(
            StringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimitingProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setResultType(List.class);
        this.tokenBucketScript.setScriptText(loadScript("ratelimit/token_bucket.lua"));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisRateLimit");
        this.redisFailures = Counter.builder("rate_limit_redis_failures_total").register(meterRegistry);
        this.redisTimer = Timer.builder("rate_limit_redis_duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.properties = properties;
    }

    public TokenBucketResult consume(TokenBucketRequest request) {
        return consumeBatch(List.of(request)).get(0);
    }

    public List<TokenBucketResult> consumeBatch(List<TokenBucketRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        Supplier<List<Object>> call = () -> {
            if (requests.size() == 1) {
                @SuppressWarnings("unchecked")
                List<Object> single = redisTemplate.execute(
                        tokenBucketScript,
                        List.of(requests.get(0).redisKey()),
                        Double.toString(requests.get(0).capacity()),
                        Double.toString(requests.get(0).refillRatePerSecond()),
                        Double.toString(requests.get(0).tokensRequired())
                );
                return Collections.singletonList(single);
            }
            return redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) {
                    for (TokenBucketRequest request : requests) {
                        operations.execute(
                                tokenBucketScript,
                                Collections.singletonList((K) request.redisKey()),
                                Double.toString(request.capacity()),
                                Double.toString(request.refillRatePerSecond()),
                                Double.toString(request.tokensRequired())
                        );
                    }
                    return null;
                }
            });
        };

        long startedAt = System.nanoTime();
        List<Object> rawResults;
        try {
            rawResults = executeWithRetry(() -> CircuitBreaker.decorateSupplier(circuitBreaker, call).get());
        } catch (Exception e) {
            redisFailures.increment();
            throw new RateLimitBackendUnavailableException(
                    "Redis rate limit backend unavailable",
                    BackendType.REDIS,
                    isRetryableRedisFailure(e),
                    e
            );
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            updateObservedLatency(elapsed);
            redisTimer.record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
        }

        List<TokenBucketResult> results = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            Object raw = rawResults != null && i < rawResults.size() ? rawResults.get(i) : null;
            results.add(mapResult(raw, requests.get(i)));
        }
        return results;
    }

    public double adaptiveScale(double maxLatencyMs, double minScale) {
        double latencyMs = observedLatencyMs.get();
        if (latencyMs <= 0 || latencyMs <= maxLatencyMs) {
            return 1.0;
        }
        double scale = maxLatencyMs / latencyMs;
        return Math.max(minScale, Math.min(1.0, scale));
    }

    private static TokenBucketResult mapResult(Object raw, TokenBucketRequest request) {
        if (!(raw instanceof List<?> result) || result.size() < 5) {
            return new TokenBucketResult(false, 0, request.capacity(), 0, 0);
        }
        boolean allowed = asLong(result.get(0)) == 1L;
        double remaining = asDouble(result.get(1));
        double limit = asDouble(result.get(2));
        long reset = asLong(result.get(3));
        long retryAfter = asLong(result.get(4));
        return new TokenBucketResult(allowed, remaining, limit, reset, retryAfter);
    }

    private void updateObservedLatency(long elapsedNanos) {
        double latestMs = elapsedNanos / 1_000_000.0;
        observedLatencyMs.updateAndGet(previous -> previous <= 0 ? latestMs : (previous * 0.8) + (latestMs * 0.2));
    }

    private <T> T executeWithRetry(Supplier<T> operation) {
        int maxAttempts = Math.max(1, properties.getFailureHandling().getRetry().getMaxAttempts());
        long delayMs = Math.max(0L, properties.getFailureHandling().getRetry().getDelayMs());
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (!isRetryableRedisFailure(ex) || attempt >= maxAttempts) {
                    throw ex;
                }
                sleepBeforeRetry(delayMs);
            }
        }
        throw lastFailure == null ? new IllegalStateException("Redis operation failed without exception") : lastFailure;
    }

    private static boolean isRetryableRedisFailure(Throwable failure) {
        if (failure instanceof RateLimitBackendUnavailableException unavailable) {
            return unavailable.retryable();
        }
        if (failure instanceof CallNotPermittedException) {
            return false;
        }
        if (failure instanceof RedisConnectionFailureException || failure instanceof QueryTimeoutException) {
            return true;
        }
        Throwable cause = failure.getCause();
        if (cause instanceof TimeoutException || cause instanceof RedisConnectionFailureException) {
            return true;
        }
        return cause != null && cause != failure && isRetryableRedisFailure(cause);
    }

    private static void sleepBeforeRetry(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
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

