package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyFactory {
    private final ClientIdentityExtractor identityExtractor;

    public RateLimitKeyFactory(ClientIdentityExtractor identityExtractor) {
        this.identityExtractor = identityExtractor;
    }

    public RateLimitKey createKey(
            RateLimitingProperties.Dimension dimension,
            String endpointPattern,
            HttpServletRequest request
    ) {
        String endpointHash = EndpointHasher.sha256Hex(Optional.ofNullable(endpointPattern).orElse("/"));

        return switch (dimension) {
            case USER -> new RateLimitKey(dimension, require(identityExtractor.extractUserIdOrNull(), "anonymous"), endpointHash);
            case IP -> new RateLimitKey(dimension, identityExtractor.extractClientIp(request), endpointHash);
            case API_KEY -> new RateLimitKey(dimension, require(identityExtractor.extractApiKeyOrNull(request), "missing"), endpointHash);
            case ENDPOINT -> new RateLimitKey(dimension, "global", endpointHash);
            case COMPOSITE -> {
                String user = identityExtractor.extractUserIdOrNull();
                String primary = (user != null) ? ("user:" + user) : ("ip:" + identityExtractor.extractClientIp(request));
                yield new RateLimitKey(dimension, primary, endpointHash);
            }
        };
    }

    private static String require(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}

