package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.annotation.RateLimitBypass;
import com.example.ratelimiting.ratelimit.annotation.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RateLimitPolicyResolver {
    private final RateLimitingProperties properties;
    private final List<HandlerMapping> handlerMappings;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public RateLimitPolicyResolver(RateLimitingProperties properties, List<HandlerMapping> handlerMappings) {
        this.properties = properties;
        this.handlerMappings = handlerMappings;
    }

    public ResolvedPolicy resolve(HttpServletRequest request) {
        String requestPath = Optional.ofNullable(request.getRequestURI()).orElse("");
        HandlerMethod handlerMethod = findHandlerMethod(request).orElse(null);

        boolean bypass = requestPath.startsWith("/actuator/");
        if (handlerMethod != null) {
            bypass = bypass
                    || hasBypass(handlerMethod.getMethod())
                    || hasBypass(handlerMethod.getBeanType());
        }

        RateLimitPolicy base = new RateLimitPolicy(
                properties.getDefaultLimits().getRequestsPerMinute(),
                properties.getDefaultLimits().getBurstCapacity(),
                properties.getDefaultLimits().getDimension(),
                "",
                bypass
        );

        RateLimited ann = handlerMethod != null ? findRateLimited(handlerMethod.getMethod()) : null;
        if (ann == null && handlerMethod != null) {
            ann = findRateLimited(handlerMethod.getBeanType());
        }

        RateLimitPolicy merged = mergeAnnotation(base, ann);

        String endpointPattern = resolveEndpointPattern(requestPath);
        if (handlerMethod != null) {
            endpointPattern = extractBestPattern(request).orElse(endpointPattern);
        }

        RateLimitingProperties.EndpointOverride override = bestOverrideMatch(endpointPattern);
        merged = applyOverride(merged, override);

        return new ResolvedPolicy(merged, endpointPattern);
    }

    private Optional<HandlerMethod> findHandlerMethod(HttpServletRequest request) {
        for (HandlerMapping mapping : handlerMappings) {
            try {
                HandlerExecutionChain chain = mapping.getHandler(request);
                if (chain == null) {
                    continue;
                }
                Object handler = chain.getHandler();
                if (handler instanceof HandlerMethod hm) {
                    return Optional.of(hm);
                }
            } catch (Exception ignored) {
                // keep trying other mappings
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractBestPattern(HttpServletRequest request) {
        Object attr = request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (attr instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    private static boolean hasBypass(java.lang.reflect.AnnotatedElement element) {
        return AnnotatedElementUtils.hasAnnotation(element, RateLimitBypass.class);
    }

    private static RateLimited findRateLimited(java.lang.reflect.AnnotatedElement element) {
        return AnnotatedElementUtils.findMergedAnnotation(element, RateLimited.class);
    }

    private static RateLimitPolicy mergeAnnotation(RateLimitPolicy base, RateLimited ann) {
        if (ann == null) {
            return base;
        }

        int rpm = base.requestsPerMinute();
        if (ann.requestsPerMinute() > 0) {
            rpm = ann.requestsPerMinute();
        } else if (ann.requestsPerHour() > 0) {
            rpm = Math.max(1, (int) Math.ceil(ann.requestsPerHour() / 60.0));
        }

        int burst = base.burstCapacity();
        if (ann.burstCapacity() > 0) {
            burst = ann.burstCapacity();
        }

        String message = base.errorMessage();
        if (ann.errorMessage() != null && !ann.errorMessage().isBlank()) {
            message = ann.errorMessage();
        }

        return new RateLimitPolicy(rpm, burst, ann.dimension(), message, base.bypass());
    }

    private RateLimitingProperties.EndpointOverride bestOverrideMatch(String endpointPatternOrPath) {
        return properties.getEndpointOverrides().stream()
                .filter(o -> antPathMatcher.match(o.getPattern(), endpointPatternOrPath))
                .max(Comparator.comparingInt(o -> o.getPattern().length()))
                .orElse(null);
    }

    private static RateLimitPolicy applyOverride(RateLimitPolicy base, RateLimitingProperties.EndpointOverride override) {
        if (override == null) {
            return base;
        }
        int rpm = override.getRequestsPerMinute() != null ? override.getRequestsPerMinute() : base.requestsPerMinute();
        int burst = override.getBurstCapacity() != null ? override.getBurstCapacity() : base.burstCapacity();
        RateLimitingProperties.Dimension dim = override.getDimension() != null ? override.getDimension() : base.dimension();
        return new RateLimitPolicy(rpm, burst, dim, base.errorMessage(), base.bypass());
    }

    private static String resolveEndpointPattern(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return "/";
        }
        return requestPath;
    }

    public record ResolvedPolicy(RateLimitPolicy policy, String endpointPattern) {
    }
}

