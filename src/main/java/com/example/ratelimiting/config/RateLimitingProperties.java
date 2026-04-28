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
    private Bypass bypass = new Bypass();

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

    public Bypass getBypass() {
        return bypass;
    }

    public void setBypass(Bypass bypass) {
        this.bypass = bypass;
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

    public enum Dimension {USER, IP, API_KEY, ENDPOINT, COMPOSITE}

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

