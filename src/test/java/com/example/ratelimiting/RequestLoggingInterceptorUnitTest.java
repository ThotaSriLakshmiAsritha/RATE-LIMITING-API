package com.example.ratelimiting;

import com.example.ratelimiting.observability.RateLimitObservation;
import com.example.ratelimiting.observability.RequestLoggingInterceptor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingInterceptorUnitTest {

    @Test
    void recordsRequestLatencyMetric() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(RateLimitObservation.RATE_LIMIT_DECISION, "ALLOWED");

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        org.assertj.core.api.Assertions.assertThat(
                meterRegistry.find("http_request_observation_duration").timer()
        ).isNotNull();
        org.assertj.core.api.Assertions.assertThat(
                meterRegistry.find("http_request_observation_duration").timer().count()
        ).isEqualTo(1);
    }
}
