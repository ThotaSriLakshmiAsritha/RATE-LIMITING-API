package com.example.ratelimiting.security;

import com.example.ratelimiting.config.RateLimitingProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IdentityResolver {
    private static final String UNKNOWN_IP = "unknown";

    private final RateLimitingProperties properties;

    public IdentityResolver(RateLimitingProperties properties) {
        this.properties = properties;
    }

    public ResolvedIdentity resolve(HttpServletRequest request) {
        String apiKey = extractApiKeyOrNull(request);
        String userId = extractUserIdOrNull();
        String clientIp = extractClientIp(request);

        if (apiKey != null) {
            return new ResolvedIdentity(IdentityType.API_KEY, apiKey, userId, apiKey, clientIp);
        }
        if (userId != null) {
            return new ResolvedIdentity(IdentityType.USER, userId, userId, null, clientIp);
        }
        return new ResolvedIdentity(IdentityType.IP, clientIp, null, null, clientIp);
    }

    public String extractUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }

    public String extractClientIp(HttpServletRequest request) {
        String remoteAddr = normalize(request.getRemoteAddr());
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = firstForwardedIp(request.getHeader("X-Forwarded-For"));
            if (forwarded != null) {
                return forwarded;
            }
            String realIp = normalize(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }
        if (remoteAddr != null) {
            return remoteAddr;
        }
        return UNKNOWN_IP;
    }

    public String extractApiKeyOrNull(HttpServletRequest request) {
        String header = properties.getApiKey().getHeader();
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        List<String> trusted = properties.getIdentity().getTrustedProxies();
        if (trusted == null || trusted.isEmpty()) {
            return false;
        }
        for (String entry : trusted) {
            String normalized = normalize(entry);
            if (normalized == null) {
                continue;
            }
            if (normalized.equals(remoteAddr)) {
                return true;
            }
            if (normalized.contains("/")) {
                if (matchesCidr(remoteAddr, normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String firstForwardedIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String candidate = normalize(part);
            if (candidate != null && !"unknown".equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static boolean matchesCidr(String ip, String cidr) {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(parts[0]);
            InetAddress candidate = InetAddress.getByName(ip);
            int prefix = Integer.parseInt(parts[1]);

            byte[] addressBytes = address.getAddress();
            byte[] candidateBytes = candidate.getAddress();
            if (addressBytes.length != candidateBytes.length) {
                return false;
            }

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addressBytes[i] != candidateBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {
                int mask = ~((1 << (8 - remainingBits)) - 1);
                int addressMasked = addressBytes[fullBytes] & mask;
                int candidateMasked = candidateBytes[fullBytes] & mask;
                return addressMasked == candidateMasked;
            }

            return true;
        } catch (NumberFormatException | UnknownHostException ex) {
            return false;
        }
    }
}
