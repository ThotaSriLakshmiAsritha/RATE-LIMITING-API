package com.example.ratelimiting;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.RateLimitPolicyResolver;
import com.example.ratelimiting.ratelimit.annotation.RateLimitBypass;
import com.example.ratelimiting.ratelimit.annotation.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

class RateLimitPolicyResolverUnitTest {

    @Test
    void methodAnnotationOverridesClassAnnotation() throws Exception {
        RateLimitingProperties props = new RateLimitingProperties();
        props.getDefaultLimits().setRequestsPerMinute(100);
        props.getDefaultLimits().setBurstCapacity(20);
        props.getDefaultLimits().setDimension(RateLimitingProperties.Dimension.IP);

        HandlerMethod hm = new HandlerMethod(new AnnotatedController(), AnnotatedController.class.getMethod("methodLimited"));
        HandlerMapping mapping = mockMapping(hm);

        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(props, List.of(mapping));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/anything");

        var resolved = resolver.resolve(req);
        org.assertj.core.api.Assertions.assertThat(resolved.policies()).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(resolved.policy().requestsPerMinute()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(resolved.policy().burstCapacity()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(resolved.policy().dimension()).isEqualTo(RateLimitingProperties.Dimension.USER);
        org.assertj.core.api.Assertions.assertThat(resolved.policies().get(0).scope().name()).isEqualTo("GLOBAL");
        org.assertj.core.api.Assertions.assertThat(resolved.policies().get(1).scope().name()).isEqualTo("ENDPOINT");
    }

    @Test
    void bypassAnnotationDisablesRateLimiting() throws Exception {
        RateLimitingProperties props = new RateLimitingProperties();

        HandlerMethod hm = new HandlerMethod(new AnnotatedController(), AnnotatedController.class.getMethod("bypassed"));
        HandlerMapping mapping = mockMapping(hm);

        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(props, List.of(mapping));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products/admin");

        var resolved = resolver.resolve(req);
        org.assertj.core.api.Assertions.assertThat(resolved.bypass()).isTrue();
    }

    @Test
    void endpointOverridesPickMostSpecificMatch() {
        RateLimitingProperties props = new RateLimitingProperties();
        RateLimitingProperties.EndpointOverride broad = new RateLimitingProperties.EndpointOverride();
        broad.setPattern("/api/**");
        broad.setRequestsPerMinute(50);
        RateLimitingProperties.EndpointOverride specific = new RateLimitingProperties.EndpointOverride();
        specific.setPattern("/api/auth/login");
        specific.setRequestsPerMinute(10);
        props.setEndpointOverrides(List.of(broad, specific));

        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(props, List.of());
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");

        var resolved = resolver.resolve(req);
        org.assertj.core.api.Assertions.assertThat(resolved.policy().requestsPerMinute()).isEqualTo(10);
    }

    private static HandlerMapping mockMapping(HandlerMethod hm) throws Exception {
        HandlerMapping mapping = Mockito.mock(HandlerMapping.class);
        Mockito.when(mapping.getHandler(Mockito.any(HttpServletRequest.class)))
                .thenAnswer(inv -> new HandlerExecutionChain(hm));
        return mapping;
    }

    @RateLimited(requestsPerMinute = 30, burstCapacity = 10, dimension = RateLimitingProperties.Dimension.IP)
    static class AnnotatedController {
        @RateLimited(requestsPerMinute = 5, burstCapacity = 2, dimension = RateLimitingProperties.Dimension.USER)
        public void methodLimited() {
        }

        @RateLimitBypass
        public void bypassed() {
        }
    }
}

