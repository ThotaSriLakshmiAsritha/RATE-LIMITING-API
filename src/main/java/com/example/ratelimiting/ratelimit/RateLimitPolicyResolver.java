package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.policy.PolicyContext;
import com.example.ratelimiting.policy.PolicyScope;
import com.example.ratelimiting.policy.PolicyService;
import com.example.ratelimiting.policy.PolicySnapshot;
import com.example.ratelimiting.policy.ResolvedPolicySnapshots;
import com.example.ratelimiting.ratelimit.annotation.RateLimitBypass;
import com.example.ratelimiting.ratelimit.annotation.RateLimited;
import com.example.ratelimiting.security.IdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RateLimitPolicyResolver {
    private final RateLimitingProperties properties;
    private final List<HandlerMapping> handlerMappings;
    private final PolicyService policyService;
    private final IdentityResolver identityResolver;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public RateLimitPolicyResolver(RateLimitingProperties properties, List<HandlerMapping> handlerMappings) {
        this.properties = properties;
        this.handlerMappings = handlerMappings;
        this.policyService = null;
        this.identityResolver = null;
    }

        @Autowired
        public RateLimitPolicyResolver(
            RateLimitingProperties properties,
            List<HandlerMapping> handlerMappings,
            ObjectProvider<PolicyService> policyServiceProvider,
            ObjectProvider<IdentityResolver> identityResolverProvider
    ) {
        this.properties = properties;
        this.handlerMappings = handlerMappings;
        this.policyService = policyServiceProvider.getIfAvailable();
        this.identityResolver = identityResolverProvider.getIfAvailable();
    }

    public ResolvedPolicy resolve(HttpServletRequest request) {
        return resolveInternal(request, true);
    }

    public ResolvedPolicy resolveWithoutDb(HttpServletRequest request) {
        return resolveInternal(request, false);
    }

    private ResolvedPolicy resolveInternal(HttpServletRequest request, boolean includeDbPolicies) {
        String requestPath = Optional.ofNullable(request.getRequestURI()).orElse("");
        HandlerMethod handlerMethod = findHandlerMethod(request).orElse(null);
        String userId = identityResolver != null ? identityResolver.resolve(request).userId() : null;

        boolean bypass = requestPath.startsWith("/actuator/");
        if (handlerMethod != null) {
            bypass = bypass
                    || hasBypass(handlerMethod.getMethod())
                    || hasBypass(handlerMethod.getBeanType());
        }

        RateLimitPolicy globalBase = new RateLimitPolicy(
                properties.getDefaultLimits().getRequestsPerMinute(),
                properties.getDefaultLimits().getBurstCapacity(),
                RateLimitingProperties.Dimension.GLOBAL,
                "",
                bypass
        );

        RateLimited ann = handlerMethod != null ? findRateLimited(handlerMethod.getMethod()) : null;
        if (ann == null && handlerMethod != null) {
            ann = findRateLimited(handlerMethod.getBeanType());
        }

        RateLimitPolicy endpointBase = new RateLimitPolicy(
                properties.getDefaultLimits().getRequestsPerMinute(),
                properties.getDefaultLimits().getBurstCapacity(),
                properties.getDefaultLimits().getDimension(),
                "",
                bypass
        );
        RateLimitPolicy endpointPolicy = mergeAnnotation(endpointBase, ann);

        String endpointPattern = resolveEndpointPattern(requestPath);
        if (handlerMethod != null) {
            endpointPattern = extractBestPattern(request).orElse(endpointPattern);
        }

        RateLimitingProperties.EndpointOverride override = bestOverrideMatch(endpointPattern);
        endpointPolicy = applyOverride(endpointPolicy, override);

        ResolvedPolicySnapshots snapshots = includeDbPolicies
                ? resolveDbPolicies(request, endpointPattern)
                : new ResolvedPolicySnapshots(null, null, null, null);
        List<ResolvedRateLimit> levels = new ArrayList<>();
        levels.add(new ResolvedRateLimit(PolicyScope.GLOBAL, applyDbPolicy(globalBase, snapshots.global(), RateLimitingProperties.Dimension.GLOBAL)));
        if (snapshots.tenant() != null) {
            levels.add(new ResolvedRateLimit(
                    PolicyScope.TENANT,
                    applyDbPolicy(globalBase, snapshots.tenant(), RateLimitingProperties.Dimension.TENANT)
            ));
        }
        if (userId != null && snapshots.user() != null) {
            levels.add(new ResolvedRateLimit(
                    PolicyScope.USER,
                    applyDbPolicy(globalBase, snapshots.user(), RateLimitingProperties.Dimension.USER)
            ));
        }
        levels.add(new ResolvedRateLimit(
                PolicyScope.ENDPOINT,
                applyDbPolicy(endpointPolicy, snapshots.endpoint(), endpointPolicy.dimension())
        ));

        return new ResolvedPolicy(levels, endpointPattern, bypass);
    }

    private Optional<HandlerMethod> findHandlerMethod(@NonNull HttpServletRequest request) {
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

    private Optional<String> extractBestPattern(@NonNull HttpServletRequest request) {
        Object attr = request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (attr instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    private static boolean hasBypass(@NonNull java.lang.reflect.AnnotatedElement element) {
        return AnnotatedElementUtils.hasAnnotation(element, RateLimitBypass.class);
    }

    private static RateLimited findRateLimited(@NonNull java.lang.reflect.AnnotatedElement element) {
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
        String path = endpointPatternOrPath == null ? "/" : endpointPatternOrPath;
        return properties.getEndpointOverrides().stream()
                .filter(o -> {
                    String pattern = o.getPattern();
                    return pattern != null && antPathMatcher.match(pattern, path);
                })
                .max(Comparator.comparingInt(o -> o.getPattern().length()))
                .orElse(null);
    }

    private static RateLimitPolicy applyOverride(RateLimitPolicy base, RateLimitingProperties.EndpointOverride override) {
        if (override == null) {
            return base;
        }
        Integer overrideRpm = override.getRequestsPerMinute();
        Integer overrideBurst = override.getBurstCapacity();
        int rpm = overrideRpm != null ? overrideRpm : base.requestsPerMinute();
        int burst = overrideBurst != null ? overrideBurst : base.burstCapacity();
        RateLimitingProperties.Dimension dim = override.getDimension() != null ? override.getDimension() : base.dimension();
        return new RateLimitPolicy(rpm, burst, dim, base.errorMessage(), base.bypass());
    }

    private ResolvedPolicySnapshots resolveDbPolicies(HttpServletRequest request, String endpointPattern) {
        if (policyService == null) {
            return new ResolvedPolicySnapshots(null, null, null, null);
        }
        String tenantId = resolveTenantId(request);
        String userId = identityResolver != null ? identityResolver.resolve(request).userId() : null;
        return policyService.resolvePolicies(new PolicyContext(tenantId, userId, endpointPattern));
    }

    private static RateLimitPolicy applyDbPolicy(
            RateLimitPolicy base,
            @Nullable PolicySnapshot dbPolicy,
            RateLimitingProperties.Dimension defaultDimension
    ) {
        if (dbPolicy == null) {
            return base;
        }
        Integer policyRpm = dbPolicy.requestsPerMinute();
        Integer policyBurst = dbPolicy.burstCapacity();
        int rpm = policyRpm != null ? policyRpm : base.requestsPerMinute();
        int burst = policyBurst != null ? policyBurst : base.burstCapacity();
        RateLimitingProperties.Dimension dim = dbPolicy.dimension() != null ? dbPolicy.dimension() : defaultDimension;
        String message = base.errorMessage();
        if (dbPolicy.errorMessage() != null && !dbPolicy.errorMessage().isBlank()) {
            message = dbPolicy.errorMessage();
        }
        return new RateLimitPolicy(rpm, burst, dim, message, base.bypass());
    }

    private static String resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader("X-Tenant-Id");
        if (header == null || header.isBlank()) {
            return "default";
        }
        return header.trim();
    }

    private static String resolveEndpointPattern(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return "/";
        }
        return requestPath;
    }

    public record ResolvedPolicy(List<ResolvedRateLimit> policies, String endpointPattern, boolean bypass) {
        public RateLimitPolicy policy() {
            return policies.isEmpty() ? null : policies.get(policies.size() - 1).policy();
        }
    }
}

