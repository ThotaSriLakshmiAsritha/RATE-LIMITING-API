package com.example.ratelimiting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {
    private boolean enabled = true;

    @NotNull
    private FallbackMode fallbackMode = FallbackMode.DENY;

    @Valid
    private ApiKey apiKey = new ApiKey();

    @Valid
    private Cors cors = new Cors();

    @Valid
    private Bypass bypass = new Bypass();

    @Valid
    private Identity identity = new Identity();

    @Valid
    private Sharding sharding = new Sharding();

    @Valid
    private Cost cost = new Cost();

    @Valid
    private Adaptive adaptive = new Adaptive();

    @Valid
    private FailureHandling failureHandling = new FailureHandling();

    @Valid
    @NotNull
    private DefaultLimits defaultLimits = new DefaultLimits();

    @Valid
    private List<EndpointOverride> endpointOverrides = List.of();

    public enum FallbackMode {ALLOW, DENY}

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FallbackMode getFallbackMode() {
        return fallbackMode;
    }

    public void setFallbackMode(FallbackMode fallbackMode) {
        this.fallbackMode = fallbackMode;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Bypass getBypass() {
        return bypass;
    }

    public void setBypass(Bypass bypass) {
        this.bypass = bypass;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Sharding getSharding() {
        return sharding;
    }

    public void setSharding(Sharding sharding) {
        this.sharding = sharding;
    }

    public Cost getCost() {
        return cost;
    }

    public void setCost(Cost cost) {
        this.cost = cost;
    }

    public Adaptive getAdaptive() {
        return adaptive;
    }

    public void setAdaptive(Adaptive adaptive) {
        this.adaptive = adaptive;
    }

    public FailureHandling getFailureHandling() {
        return failureHandling;
    }

    public void setFailureHandling(FailureHandling failureHandling) {
        this.failureHandling = failureHandling;
    }

    public DefaultLimits getDefaultLimits() {
        return defaultLimits;
    }

    public void setDefaultLimits(DefaultLimits defaultLimits) {
        this.defaultLimits = defaultLimits;
    }

    public List<EndpointOverride> getEndpointOverrides() {
        return endpointOverrides;
    }

    public void setEndpointOverrides(List<EndpointOverride> endpointOverrides) {
        this.endpointOverrides = endpointOverrides;
    }

    public static class ApiKey {
        @NotNull
        private String header = "X-API-Key";

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }
    }

    public static class Bypass {
        @NotNull
        private List<String> ipWhitelist = List.of();
        @NotNull
        private List<String> apiKeyWhitelist = List.of();

        public List<String> getIpWhitelist() {
            return ipWhitelist;
        }

        public void setIpWhitelist(List<String> ipWhitelist) {
            this.ipWhitelist = ipWhitelist;
        }

        public List<String> getApiKeyWhitelist() {
            return apiKeyWhitelist;
        }

        public void setApiKeyWhitelist(List<String> apiKeyWhitelist) {
            this.apiKeyWhitelist = apiKeyWhitelist;
        }
    }

    public static class Identity {
        @NotNull
        private List<String> trustedProxies = List.of();

        public List<String> getTrustedProxies() {
            return trustedProxies;
        }

        public void setTrustedProxies(List<String> trustedProxies) {
            this.trustedProxies = trustedProxies;
        }
    }

    public static class Sharding {
        private boolean enabled = false;
        private int shardCount = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getShardCount() {
            return shardCount;
        }

        public void setShardCount(int shardCount) {
            this.shardCount = shardCount;
        }
    }

    public static class Cost {
        @NotNull
        private String header = "X-RateLimit-Cost";
        private double min = 1.0;
        private double max = 10.0;

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }
    }

    public static class Adaptive {
        private boolean enabled = false;
        private double maxLatencyMs = 10.0;
        private double minScale = 0.5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getMaxLatencyMs() {
            return maxLatencyMs;
        }

        public void setMaxLatencyMs(double maxLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
        }

        public double getMinScale() {
            return minScale;
        }

        public void setMinScale(double minScale) {
            this.minScale = minScale;
        }
    }

    public static class FailureHandling {
        private FallbackMode redisFallbackMode;
        private FallbackMode dbFallbackMode;
        private FallbackMode unexpectedFallbackMode = FallbackMode.DENY;

        @Valid
        private Retry retry = new Retry();

        public FallbackMode getRedisFallbackMode() {
            return redisFallbackMode;
        }

        public void setRedisFallbackMode(FallbackMode redisFallbackMode) {
            this.redisFallbackMode = redisFallbackMode;
        }

        public FallbackMode getDbFallbackMode() {
            return dbFallbackMode;
        }

        public void setDbFallbackMode(FallbackMode dbFallbackMode) {
            this.dbFallbackMode = dbFallbackMode;
        }

        public FallbackMode getUnexpectedFallbackMode() {
            return unexpectedFallbackMode;
        }

        public void setUnexpectedFallbackMode(FallbackMode unexpectedFallbackMode) {
            this.unexpectedFallbackMode = unexpectedFallbackMode;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }
    }

    public static class Cors {
        @NotNull
        private List<String> allowedOrigins = List.of();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Retry {
        private int maxAttempts = 2;
        private long delayMs = 25;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }
    }

    public enum Dimension {GLOBAL, TENANT, USER, IP, API_KEY, ENDPOINT, COMPOSITE}

    public static class DefaultLimits {
        private int requestsPerMinute = 100;
        private int burstCapacity = 20;
        @NotNull
        private Dimension dimension = Dimension.IP;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public Dimension getDimension() {
            return dimension;
        }

        public void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }
    }

    public static class EndpointOverride {
        @NotNull
        private String pattern;
        private Integer requestsPerMinute;
        private Integer burstCapacity;
        private Dimension dimension;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public Integer getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(Integer requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public Integer getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(Integer burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public Dimension getDimension() {
            return dimension;
        }

        public void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }
    }
}

