package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ClientIdentityExtractor {
    private final RateLimitingProperties properties;

    public ClientIdentityExtractor(RateLimitingProperties properties) {
        this.properties = properties;
    }

    public String extractUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }

    public String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }

    public String extractApiKeyOrNull(HttpServletRequest request) {
        String header = properties.getApiKey().getHeader();
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

