package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.EndpointHasher;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
import com.example.ratelimiting.policy.PolicyScope;
import com.example.ratelimiting.security.IdentityResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitKeyFactoryUnitTest {
    @Test
    void endpointHasherIsDeterministicSha256Hex() {
        String h1 = EndpointHasher.sha256Hex("/api/v1/products");
        String h2 = EndpointHasher.sha256Hex("/api/v1/products");
        org.assertj.core.api.Assertions.assertThat(h1).isEqualTo(h2);
        org.assertj.core.api.Assertions.assertThat(h1).hasSize(64);
    }

    @Test
    void endpointScopeUsesClientIpAndEndpointHash() {
        RateLimitingProperties props = new RateLimitingProperties();
        props.getIdentity().setTrustedProxies(List.of("127.0.0.1"));
        IdentityResolver resolver = new IdentityResolver(props);
        RateLimitKeyFactory factory = new RateLimitKeyFactory(resolver, props);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "10.0.0.1");

        var key = factory.createKey(PolicyScope.ENDPOINT, RateLimitingProperties.Dimension.IP, "/api/v1/products", req);
        org.assertj.core.api.Assertions.assertThat(key.toRedisKey()).startsWith("rate_limit:IP:10.0.0.1:");
    }

    @Test
    void globalScopeDoesNotIncludeEndpointHash() {
        RateLimitingProperties props = new RateLimitingProperties();
        IdentityResolver resolver = new IdentityResolver(props);
        RateLimitKeyFactory factory = new RateLimitKeyFactory(resolver, props);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        var key = factory.createKey(PolicyScope.GLOBAL, RateLimitingProperties.Dimension.GLOBAL, "/api/v1/products", req);

        org.assertj.core.api.Assertions.assertThat(key.toRedisKey()).isEqualTo("rate_limit:GLOBAL:global:all:s0");
    }
}

