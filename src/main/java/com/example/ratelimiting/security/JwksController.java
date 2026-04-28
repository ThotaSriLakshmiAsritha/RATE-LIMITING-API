package com.example.ratelimiting.security;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
public class JwksController {
    private final JwksService jwksService;

    public JwksController(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwksService.publicJwkSet().toJSONObject();
    }
}

