package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.ClientIdentityExtractor;
import com.example.ratelimiting.ratelimit.EndpointHasher;
import com.example.ratelimiting.ratelimit.RateLimitKeyFactory;
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
    void ipDimensionUsesClientIpAndEndpointHash() {
        RateLimitingProperties props = new RateLimitingProperties();
        ClientIdentityExtractor extractor = new ClientIdentityExtractor(props);
        RateLimitKeyFactory factory = new RateLimitKeyFactory(extractor);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("X-Forwarded-For", "10.0.0.1");

        var key = factory.createKey(RateLimitingProperties.Dimension.IP, "/api/v1/products", req);
        org.assertj.core.api.Assertions.assertThat(key.toRedisKey()).startsWith("rate_limit:IP:10.0.0.1:");
    }
}

