package com.example.ratelimiting.policy;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.BackendType;
import com.example.ratelimiting.ratelimit.RateLimitBackendUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Service
public class PolicyService {
    private static final String DEFAULT_TENANT = "default";

    private final PolicyRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PolicyCacheProperties properties;
    private final RateLimitingProperties rateLimitingProperties;
    private final CircuitBreaker dbCircuitBreaker;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final Map<String, CacheEntry<PolicySnapshot>> policyCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<PolicySnapshot>>> endpointCache = new ConcurrentHashMap<>();

    public PolicyService(
            PolicyRepository repository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PolicyCacheProperties properties,
            RateLimitingProperties rateLimitingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rateLimitingProperties = rateLimitingProperties;
        this.dbCircuitBreaker = circuitBreakerRegistry.circuitBreaker("dbPolicyLookup");
    }

    public ResolvedPolicySnapshots resolvePolicies(PolicyContext context) {
        String tenantId = normalizeTenant(context.tenantId());
        PolicySnapshot global = loadPolicySnapshot(tenantId, PolicyScope.GLOBAL, null);
        PolicySnapshot tenant = loadPolicySnapshot(tenantId, PolicyScope.TENANT, tenantId);
        PolicySnapshot user = context.userId() != null
                ? loadPolicySnapshot(tenantId, PolicyScope.USER, context.userId())
                : null;
        PolicySnapshot endpoint = loadEndpointPolicy(tenantId, context.endpointPattern());
        return new ResolvedPolicySnapshots(global, tenant, user, endpoint);
    }

    public PolicySnapshot resolvePolicy(PolicyContext context) {
        ResolvedPolicySnapshots snapshots = resolvePolicies(context);
        if (snapshots.endpoint() != null) {
            return snapshots.endpoint();
        }
        if (snapshots.user() != null) {
            return snapshots.user();
        }
        if (snapshots.tenant() != null) {
            return snapshots.tenant();
        }
        return snapshots.global();
    }

    public PolicyEntity save(@NonNull PolicyEntity policy) {
        PolicyEntity saved = repository.save(policy);
        refreshCacheForPolicy(saved);
        publishInvalidation(saved.getTenantId(), saved.getScopeType(), saved.getScopeId());
        return saved;
    }

    public void delete(@NonNull UUID policyId) {
        Optional<PolicyEntity> policy = repository.findById(policyId);
        if (policy.isPresent()) {
            PolicyEntity entity = policy.get();
            repository.deleteById(policyId);
            refreshCacheForPolicy(entity);
            publishInvalidation(entity.getTenantId(), entity.getScopeType(), entity.getScopeId());
        }
    }

    void handleInvalidation(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 2) {
            return;
        }
        String tenantId = parts[0];
        PolicyScope scopeType;
        try {
            scopeType = PolicyScope.valueOf(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        String scopeId = parts.length == 3 ? parts[2] : null;
        evictLocalCache(tenantId, scopeType, scopeId);
    }

    private PolicySnapshot loadPolicySnapshot(String tenantId, PolicyScope scopeType, String scopeId) {
        String cacheKey = cacheKey(tenantId, scopeType, scopeId);
        PolicySnapshot cached = getCached(policyCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        PolicySnapshot redisCached = readPolicyFromRedis(cacheKey);
        if (redisCached != null) {
            putCached(policyCache, cacheKey, redisCached);
            return redisCached;
        }

        List<PolicyEntity> policies;
        if (scopeId == null) {
            policies = runDbLookup(() -> repository.findByTenantIdAndScopeTypeAndEnabledTrueOrderByPriorityDesc(tenantId, scopeType));
        } else {
            policies = runDbLookup(() -> repository.findByTenantIdAndScopeTypeAndScopeIdAndEnabledTrueOrderByPriorityDesc(
                    tenantId, scopeType, scopeId
            ));
        }
        PolicySnapshot snapshot = policies.isEmpty() ? null : toSnapshot(policies.get(0));
        if (snapshot != null) {
            writePolicyToRedis(cacheKey, snapshot);
        }
        putCached(policyCache, cacheKey, snapshot);
        return snapshot;
    }

    private PolicySnapshot loadEndpointPolicy(String tenantId, String endpointPattern) {
        if (endpointPattern == null || endpointPattern.isBlank()) {
            return null;
        }
        String cacheKey = endpointCacheKey(tenantId);
        List<PolicySnapshot> cached = getCached(endpointCache, cacheKey);
        if (cached == null) {
            List<PolicySnapshot> redisCached = readEndpointListFromRedis(cacheKey);
            if (redisCached != null) {
                cached = redisCached;
                putCached(endpointCache, cacheKey, cached);
            }
        }
        if (cached == null) {
            List<PolicyEntity> policies = runDbLookup(() -> repository.findByTenantIdAndScopeTypeAndEnabledTrueOrderByPriorityDesc(
                    tenantId, PolicyScope.ENDPOINT
            ));
            cached = new ArrayList<>();
            for (PolicyEntity policy : policies) {
                cached.add(toSnapshot(policy));
            }
            writeEndpointListToRedis(cacheKey, cached);
            putCached(endpointCache, cacheKey, cached);
        }

        return cached.stream()
            .filter(p -> matchesEndpoint(p, endpointPattern))
                .max(Comparator
                        .comparingInt((PolicySnapshot p) -> Optional.ofNullable(p.priority()).orElse(0))
                        .thenComparingInt(p -> Optional.ofNullable(p.endpointPattern()).orElse("").length())
                )
                .orElse(null);
    }

    private void refreshCacheForPolicy(PolicyEntity policy) {
        if (policy.getScopeType() == PolicyScope.ENDPOINT) {
            endpointCache.remove(endpointCacheKey(policy.getTenantId()));
            if (properties.isRedisEnabled()) {
                try {
                    redisTemplate.delete(redisKey(endpointCacheKey(policy.getTenantId())));
                } catch (RuntimeException ignored) {
                    // best effort cache eviction
                }
            }
            return;
        }
        String key = cacheKey(policy.getTenantId(), policy.getScopeType(), policy.getScopeId());
        PolicySnapshot snapshot = policy.isEnabled() ? toSnapshot(policy) : null;
        putCached(policyCache, key, snapshot);
        if (snapshot != null) {
            writePolicyToRedis(key, snapshot);
        } else if (properties.isRedisEnabled()) {
            try {
                redisTemplate.delete(redisKey(key));
            } catch (RuntimeException ignored) {
                // best effort cache eviction
            }
        }
    }

    private void publishInvalidation(String tenantId, PolicyScope scopeType, String scopeId) {
        if (!properties.isRedisEnabled()) {
            return;
        }
        String payload = tenantId + "|" + scopeType.name() + "|" + (scopeId == null ? "" : scopeId);
        String channel = Objects.requireNonNull(properties.getInvalidationChannel());
        try {
            redisTemplate.convertAndSend(channel, payload);
        } catch (RuntimeException ignored) {
            // best effort invalidation
        }
    }

    private void evictLocalCache(String tenantId, PolicyScope scopeType, String scopeId) {
        if (scopeType == PolicyScope.ENDPOINT) {
            endpointCache.remove(endpointCacheKey(tenantId));
            return;
        }
        String key = cacheKey(tenantId, scopeType, scopeId);
        policyCache.remove(key);
    }

    private String cacheKey(String tenantId, PolicyScope scopeType, String scopeId) {
        String normalizedScopeId = (scopeId == null || scopeId.isBlank()) ? "_" : scopeId;
        return tenantId + ":" + scopeType.name() + ":" + normalizedScopeId;
    }

    private String endpointCacheKey(String tenantId) {
        return tenantId + ":" + PolicyScope.ENDPOINT.name() + ":list";
    }

    private PolicySnapshot readPolicyFromRedis(String cacheKey) {
        if (!properties.isRedisEnabled()) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(redisKey(cacheKey));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, PolicySnapshot.class);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private void writePolicyToRedis(String cacheKey, PolicySnapshot snapshot) {
        if (!properties.isRedisEnabled()) {
            return;
        }
        try {
            String raw = Objects.requireNonNull(objectMapper.writeValueAsString(snapshot));
            String key = Objects.requireNonNull(redisKey(cacheKey));
            Duration ttl = Objects.requireNonNull(cacheTtl());
            redisTemplate.opsForValue().set(key, raw, ttl);
        } catch (JsonProcessingException | RuntimeException ignored) {
            // best effort
        }
    }

    private List<PolicySnapshot> readEndpointListFromRedis(String cacheKey) {
        if (!properties.isRedisEnabled()) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(redisKey(cacheKey));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, new TypeReference<List<PolicySnapshot>>() {});
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private void writeEndpointListToRedis(String cacheKey, List<PolicySnapshot> snapshots) {
        if (!properties.isRedisEnabled()) {
            return;
        }
        try {
            String raw = Objects.requireNonNull(objectMapper.writeValueAsString(snapshots));
            String key = Objects.requireNonNull(redisKey(cacheKey));
            Duration ttl = Objects.requireNonNull(cacheTtl());
            redisTemplate.opsForValue().set(key, raw, ttl);
        } catch (JsonProcessingException | RuntimeException ignored) {
            // best effort
        }
    }

    @NonNull
    private String redisKey(String cacheKey) {
        return Objects.requireNonNull(properties.getRedisPrefix()) + Objects.requireNonNull(cacheKey);
    }

    private static <T> T getCached(Map<String, CacheEntry<T>> cache, String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    private <T> void putCached(Map<String, CacheEntry<T>> cache, String key, T value) {
        Duration ttl = cacheTtl();
        Instant expiresAt = Instant.now().plus(ttl);
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    private PolicySnapshot toSnapshot(PolicyEntity entity) {
        return new PolicySnapshot(
                entity.getId(),
                entity.getTenantId(),
                entity.getScopeType(),
                entity.getScopeId(),
                entity.getEndpointPattern(),
                entity.getRequestsPerMinute(),
                entity.getBurstCapacity(),
                entity.getDimension(),
                entity.getErrorMessage(),
                entity.getMode(),
                entity.getPriority(),
                entity.getVersion()
        );
    }

    private static String normalizeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return DEFAULT_TENANT;
        }
        return tenantId.trim();
    }

    private Duration cacheTtl() {
        Duration ttl = properties.getTtl();
        return ttl != null ? ttl : Duration.ofSeconds(60);
    }

    private boolean matchesEndpoint(PolicySnapshot snapshot, String endpointPattern) {
        String candidate = snapshot.endpointPattern();
        if (candidate == null || endpointPattern == null) {
            return false;
        }
        return antPathMatcher.match(candidate, endpointPattern);
    }

    private <T> T runDbLookup(Supplier<T> operation) {
        int maxAttempts = Math.max(1, rateLimitingProperties.getFailureHandling().getRetry().getMaxAttempts());
        long delayMs = Math.max(0L, rateLimitingProperties.getFailureHandling().getRetry().getDelayMs());
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Supplier<T> protectedCall = CircuitBreaker.decorateSupplier(dbCircuitBreaker, operation::get);
                return protectedCall.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (!isRetryableDbFailure(ex) || attempt >= maxAttempts) {
                    throw new RateLimitBackendUnavailableException(
                            "Database policy backend unavailable",
                            BackendType.DATABASE,
                            isRetryableDbFailure(ex),
                            ex
                    );
                }
                sleepBeforeRetry(delayMs);
            }
        }
        throw new RateLimitBackendUnavailableException(
                "Database policy backend unavailable",
                BackendType.DATABASE,
                true,
                lastFailure
        );
    }

    private static boolean isRetryableDbFailure(Throwable failure) {
        if (failure instanceof RateLimitBackendUnavailableException unavailable) {
            return unavailable.retryable();
        }
        if (failure instanceof CallNotPermittedException) {
            return false;
        }
        if (failure instanceof CannotGetJdbcConnectionException
                || failure instanceof QueryTimeoutException
                || failure instanceof CannotAcquireLockException
                || failure instanceof TransientDataAccessException) {
            return true;
        }
        Throwable cause = failure.getCause();
        if (cause instanceof TimeoutException || cause instanceof DataAccessException) {
            return cause instanceof TransientDataAccessException
                    || cause instanceof CannotGetJdbcConnectionException
                    || cause instanceof QueryTimeoutException
                    || cause instanceof CannotAcquireLockException;
        }
        return cause != null && cause != failure && isRetryableDbFailure(cause);
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

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
