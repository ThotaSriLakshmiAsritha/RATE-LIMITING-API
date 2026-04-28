package com.example.ratelimiting;

import com.example.ratelimiting.testsupport.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitingIntegrationTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    @Test
    void loginIsRateLimitedByIpBurst3() {
        Map<String, String> payload = Map.of("username", "demo", "password", "password");

        ResponseEntity<Map> r1 = postJson("/api/auth/login", payload);
        ResponseEntity<Map> r2 = postJson("/api/auth/login", payload);
        ResponseEntity<Map> r3 = postJson("/api/auth/login", payload);
        ResponseEntity<Map> r4 = postJson("/api/auth/login", payload);

        org.assertj.core.api.Assertions.assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(r4.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        org.assertj.core.api.Assertions.assertThat(r4.getHeaders().getFirst("Retry-After")).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(r4.getHeaders().getFirst("X-RateLimit-Limit")).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(r4.getHeaders().getFirst("X-RateLimit-Remaining")).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(r4.getHeaders().getFirst("X-RateLimit-Reset")).isNotBlank();
    }

    @Test
    void productsAreAccessibleWithJwt() {
        String token = authToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = rest.exchange("/api/v1/products", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        org.assertj.core.api.Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(resp.getHeaders().getFirst("X-RateLimit-Limit")).isNotBlank();
    }

    @Test
    void dashboardPoliciesCanBeCreatedUpdatedAndListed() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> createPayload = Map.of(
                "tenantId", "default",
                "scopeType", "TENANT",
                "scopeId", "default",
                "requestsPerMinute", 120,
                "burstCapacity", 30,
                "dimension", "TENANT",
                "priority", 10,
                "enabled", true
        );

        ResponseEntity<Map> created = rest.exchange(
                "/api/dashboard/policies",
                HttpMethod.POST,
                new HttpEntity<>(createPayload, headers),
                Map.class
        );
        org.assertj.core.api.Assertions.assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(created.getBody()).isNotNull();

        String policyId = created.getBody().get("id").toString();
        Map<String, Object> updatePayload = Map.of(
                "tenantId", "default",
                "scopeType", "TENANT",
                "scopeId", "default",
                "requestsPerMinute", 150,
                "burstCapacity", 40,
                "dimension", "TENANT",
                "priority", 20,
                "enabled", true
        );

        ResponseEntity<Map> updated = rest.exchange(
                "/api/dashboard/policies/" + policyId,
                HttpMethod.PUT,
                new HttpEntity<>(updatePayload, headers),
                Map.class
        );
        org.assertj.core.api.Assertions.assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(updated.getBody().get("requestsPerMinute")).isEqualTo(150);

        ResponseEntity<List> listed = rest.exchange(
                "/api/dashboard/policies?tenantId=default",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                List.class
        );
        org.assertj.core.api.Assertions.assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(listed.getBody()).isNotEmpty();
    }

    @Test
    void dashboardAnalyticsAreAvailable() {
        ResponseEntity<Map> response = rest.exchange(
                "/api/dashboard/analytics",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).containsKeys("generatedAt", "rateLimitStats", "usageMetrics");
    }

    private ResponseEntity<Map> postJson(String path, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken());
        return headers;
    }

    private String authToken() {
        return (String) postJson("/api/auth/login", Map.of("username", "demo", "password", "password"))
                .getBody()
                .get("access_token");
    }
}

