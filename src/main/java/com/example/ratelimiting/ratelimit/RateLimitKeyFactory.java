package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.policy.PolicyScope;
import com.example.ratelimiting.security.IdentityResolver;
import com.example.ratelimiting.security.ResolvedIdentity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyFactory {
    private final IdentityResolver identityResolver;
    private final RateLimitingProperties properties;

    public RateLimitKeyFactory(IdentityResolver identityResolver, RateLimitingProperties properties) {
        this.identityResolver = identityResolver;
        this.properties = properties;
    }

    public RateLimitKey createKey(
            PolicyScope scope,
            RateLimitingProperties.Dimension dimension,
            String endpointPattern,
            HttpServletRequest request
    ) {
        ResolvedIdentity identity = identityResolver.resolve(request);
        String tenantId = resolveTenantId(request);
        String endpointHash = scope == PolicyScope.ENDPOINT
                ? EndpointHasher.sha256Hex(Optional.ofNullable(endpointPattern).orElse("/"))
                : "all";
        int shard = resolveShard(scope, dimension, endpointPattern, identity, tenantId);

        return switch (scope) {
            case GLOBAL -> new RateLimitKey(RateLimitingProperties.Dimension.GLOBAL, "global", endpointHash, shard);
            case TENANT -> new RateLimitKey(RateLimitingProperties.Dimension.TENANT, require(tenantId, "default"), endpointHash, shard);
            case USER -> new RateLimitKey(RateLimitingProperties.Dimension.USER, require(identity.userId(), "anonymous"), endpointHash, shard);
            case ENDPOINT -> createEndpointKey(dimension, endpointHash, shard, identity, tenantId);
        };
    }

    private static RateLimitKey createEndpointKey(
            RateLimitingProperties.Dimension dimension,
            String endpointHash,
            int shard,
            ResolvedIdentity identity,
            String tenantId
    ) {
        return switch (dimension) {
            case USER -> new RateLimitKey(dimension, require(identity.userId(), "anonymous"), endpointHash, shard);
            case IP -> new RateLimitKey(dimension, require(identity.clientIp(), "unknown"), endpointHash, shard);
            case API_KEY -> new RateLimitKey(dimension, require(identity.apiKey(), "missing"), endpointHash, shard);
            case TENANT -> new RateLimitKey(dimension, require(tenantId, "default"), endpointHash, shard);
            case GLOBAL, ENDPOINT -> new RateLimitKey(RateLimitingProperties.Dimension.ENDPOINT, require(tenantId, "default"), endpointHash, shard);
            case COMPOSITE -> {
                String user = identity.userId();
                String primary = (user != null) ? ("user:" + user) : ("ip:" + require(identity.clientIp(), "unknown"));
                yield new RateLimitKey(dimension, primary, endpointHash, shard);
            }
        };
    }

    private static String require(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public int effectiveShardCount(PolicyScope scope) {
        if (!supportsSharding(scope)) {
            return 1;
        }
        RateLimitingProperties.Sharding sharding = properties.getSharding();
        if (sharding == null || !sharding.isEnabled()) {
            return 1;
        }
        return Math.max(1, sharding.getShardCount());
    }

    private int resolveShard(
            PolicyScope scope,
            RateLimitingProperties.Dimension dimension,
            String endpointPattern,
            ResolvedIdentity identity,
            String tenantId
    ) {
        int shardCount = effectiveShardCount(scope);
        if (shardCount == 1) {
            return 0;
        }
        String seed = switch (scope) {
            case GLOBAL, TENANT, ENDPOINT -> shardSeed(identity, tenantId, endpointPattern);
            case USER -> require(identity.userId(), "anonymous");
        };
        int hash = (dimension.name() + "|" + seed).hashCode();
        return Math.floorMod(hash, shardCount);
    }

    private static boolean supportsSharding(PolicyScope scope) {
        return scope == PolicyScope.GLOBAL || scope == PolicyScope.TENANT || scope == PolicyScope.ENDPOINT;
    }

    private static String shardSeed(ResolvedIdentity identity, String tenantId, String endpointPattern) {
        if (identity.userId() != null && !identity.userId().isBlank()) {
            return identity.userId();
        }
        if (identity.apiKey() != null && !identity.apiKey().isBlank()) {
            return identity.apiKey();
        }
        if (identity.clientIp() != null && !identity.clientIp().isBlank()) {
            return identity.clientIp();
        }
        return require(tenantId, "default") + "|" + Optional.ofNullable(endpointPattern).orElse("/");
    }

    private static String resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader("X-Tenant-Id");
        if (header == null || header.isBlank()) {
            return "default";
        }
        return header.trim();
    }
}

