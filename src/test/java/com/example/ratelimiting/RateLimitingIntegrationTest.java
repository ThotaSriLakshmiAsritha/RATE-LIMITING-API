package com.example.ratelimiting;

import com.example.ratelimiting.testsupport.IntegrationTestBase;
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
        String token = (String) postJson("/api/auth/login", Map.of("username", "demo", "password", "password"))
                .getBody()
                .get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = rest.exchange("/api/v1/products", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        org.assertj.core.api.Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(resp.getHeaders().getFirst("X-RateLimit-Limit")).isNotBlank();
    }

    private ResponseEntity<Map> postJson(String path, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
    }
}

